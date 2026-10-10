package com.ahmedbecett.filemaster

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.Locale

private data class BatterySnapshot(val level: Int, val temp: Float?, val voltage: Float?, val charging: Boolean, val health: String)
private data class FileScan(val files: Int, val folders: Int, val bytes: Long, val junk: List<String>, val largest: List<Pair<String, Long>>)

private fun bytesLabel(bytes: Long): String {
    if (bytes < 0) return "غير متاح"
    var amount = bytes.toDouble()
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var unit = 0
    while (amount >= 1024 && unit < units.lastIndex) { amount /= 1024; unit++ }
    return String.format(Locale.getDefault(), if (unit >= 2) "%.1f %s" else "%.0f %s", amount, units[unit])
}

private fun batterySnapshot(context: Context, intent: Intent? = null): BatterySnapshot {
    val i = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    if (i == null) return BatterySnapshot(0, null, null, false, "غير معروف")
    val level = i.getIntExtra("level", 0)
    val scale = i.getIntExtra("scale", 100).coerceAtLeast(1)
    val status = i.getIntExtra("status", -1)
    val health = when (i.getIntExtra("health", -1)) {
        2 -> "جيدة"; 3 -> "تحتاج فحصاً"; 4 -> "معطوبة"; 5 -> "مرتفعة الحرارة"; else -> "غير معروف"
    }
    val t = i.getIntExtra("temperature", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }?.div(10f)
    val v = i.getIntExtra("voltage", 0).takeIf { it > 0 }?.div(1000f)
    return BatterySnapshot((level * 100 / scale).coerceIn(0, 100), t, v, status == 2 || status == 5, health)
}

private suspend fun scanFolder(folder: DocumentFile, update: suspend (Int, Int) -> Unit): FileScan = withContext(Dispatchers.IO) {
    val pending = ArrayDeque<DocumentFile>()
    val junk = mutableListOf<String>()
    val largest = mutableListOf<Pair<String, Long>>()
    var files = 0
    var folders = 0
    var bytes = 0L
    pending.add(folder)
    val extensions = setOf("tmp", "temp", "bak", "old", "part", "crdownload", "log", "cache", "dmp")
    while (pending.isNotEmpty()) {
        val dir = pending.removeLast()
        val children = try { dir.listFiles() } catch (_: Exception) { emptyArray() }
        for (f in children) {
            if (f.isDirectory) {
                folders++
                if (f.name != ".FileMasterTrash" && f.name != ".FileMasterHidden") pending.add(f)
            } else if (f.isFile) {
                files++
                val size = try { f.length().coerceAtLeast(0) } catch (_: Exception) { 0L }
                bytes += size
                val name = f.name ?: "ملف بلا اسم"
                val lower = name.lowercase(Locale.ROOT)
                if (lower.substringAfterLast('.', "") in extensions || lower == "thumbs.db" || lower == ".ds_store" || lower.startsWith("~$")) {
                    junk.add("$name • ${bytesLabel(size)}")
                }
                if (size > 0) {
                    largest.add(name to size)
                    if (largest.size > 40) { largest.sortByDescending { it.second }; while (largest.size > 10) largest.removeAt(largest.lastIndex) }
                }
                if (files % 16 == 0) withContext(Dispatchers.Main) { update(files, folders) }
            }
        }
    }
    largest.sortByDescending { it.second }
    FileScan(files, folders, bytes, junk, largest.take(5))
}

@Composable
fun DeviceHealthDashboard(context: Context, folder: DocumentFile?, darkMode: Boolean) {
    var battery by remember { mutableStateOf(batterySnapshot(context)) }
    var ram by remember { mutableStateOf("جارٍ القراءة…") }
    var storage by remember { mutableStateOf("جارٍ القراءة…") }
    var thermal by remember { mutableStateOf("غير متاح") }
    var scanning by remember { mutableStateOf(false) }
    var fileCount by remember { mutableIntStateOf(0) }
    var folderCount by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<FileScan?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val level by animateFloatAsState(battery.level / 100f, label = "battery")
    val card = if (darkMode) Color(0xFF142033) else Color.White
    val foreground = if (darkMode) Color(0xFFF1F5F9) else Color(0xFF172235)
    val muted = if (darkMode) Color(0xFFA9B7C8) else Color(0xFF5B6B7F)
    val teal = Color(0xFF39D7C3)
    val violet = Color(0xFF8D8BFF)

    fun refresh() {
        battery = batterySnapshot(context)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        ram = "${bytesLabel(info.availMem)} متاحة من ${bytesLabel(info.totalMem)}"
        runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            storage = "${bytesLabel(stat.availableBytes)} متاحة من ${bytesLabel(stat.totalBytes)}"
        }.onFailure { storage = "غير متاح" }
        thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "طبيعية"
                PowerManager.THERMAL_STATUS_LIGHT -> "ارتفاع طفيف"
                PowerManager.THERMAL_STATUS_MODERATE -> "مرتفعة — خفّف الحمل"
                PowerManager.THERMAL_STATUS_SEVERE -> "مرتفعة جداً — أوقف المهام الثقيلة"
                PowerManager.THERMAL_STATUS_CRITICAL -> "حرجة — اترك الهاتف يبرد"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "طوارئ حرارية"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "إيقاف حراري وشيك"
                else -> "غير متاح"
            }
        } else "يتطلب Android 10 أو أحدث"
    }

    LaunchedEffect(Unit) { refresh() }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { battery = batterySnapshot(context, intent) }
        }
        runCatching { context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(teal.copy(alpha = .24f), violet.copy(alpha = .20f)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Memory, null, tint = teal)
            }
            Column(Modifier.weight(1f)) {
                Text("مركز صحة الهاتف", color = foreground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("قياسات فعلية من نظام Android", color = muted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { refresh() }) { Text("تحديث", color = teal) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(25.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { level }, modifier = Modifier.fillMaxSize(), color = if (battery.level < 16) Color(0xFFFF7070) else teal, trackColor = muted.copy(alpha = .18f), strokeWidth = 8.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.BatteryChargingFull, null, tint = teal, modifier = Modifier.size(22.dp))
                            Text("${battery.level}%", color = foreground, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (battery.charging) "البطارية قيد الشحن" else "البطارية", color = foreground, fontWeight = FontWeight.SemiBold)
                        Text("الصحة: ${battery.health}", color = muted, style = MaterialTheme.typography.bodySmall)
                        Text("الجهد: ${battery.voltage?.let { String.format(Locale.getDefault(), "%.2f V", it) } ?: "غير متاح"}", color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = muted.copy(alpha = .18f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("RAM", ram, Icons.Default.Memory, Modifier.weight(1f), foreground, muted, teal)
                    Metric("التخزين", storage, Icons.Default.Storage, Modifier.weight(1f), foreground, muted, violet)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.DeviceThermostat, null, tint = if (thermal.contains("حرجة") || thermal.contains("جداً")) Color(0xFFFF7070) else teal)
                    Column(Modifier.weight(1f)) {
                        Text("حرارة النظام", color = foreground, fontWeight = FontWeight.SemiBold)
                        Text(thermal, color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(battery.temp?.let { String.format(Locale.getDefault(), "%.1f°C", it) } ?: "—°C", color = foreground, fontWeight = FontWeight.Bold)
                }
                Text("درجة الحرارة المعروضة هي حرارة البطارية من النظام عند توفرها. لا يسمح Android عادةً للتطبيقات بالتحكم في مروحة داخلية؛ تُعرض حالة الحرارة والتنبيهات فقط.", color = muted, style = MaterialTheme.typography.labelSmall)
                if (thermal.contains("مرتفعة") || thermal.contains("حرجة") || thermal.contains("طوارئ")) {
                    Text("لتبريد الهاتف: أوقف الألعاب والمهام الثقيلة، أبعده عن الشمس، واتركه يبرد. افصل الشاحن إذا كان ذلك آمناً.", color = Color(0xFFFF9D91), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = card), shape = RoundedCornerShape(25.dp)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Search, null, tint = teal)
                    Column(Modifier.weight(1f)) {
                        Text("الفحص الذكي للملفات", color = foreground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(folder?.name ?: "اختر مجلداً من مستكشف الملفات أولاً", color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("يفحص شجرة المجلدات فعلياً، ويحسب الحجم ويعرض الملفات المؤقتة المرشحة وأكبر الملفات. لا يحذف شيئاً تلقائياً.", color = muted, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        val target = folder
                        if (target == null) error = "افتح مستكشف الملفات واختر مجلداً أولاً، ثم أعد الفحص."
                        else {
                            scanning = true; fileCount = 0; folderCount = 0; result = null; error = null
                            scope.launch {
                                try { result = scanFolder(target) { f, d -> fileCount = f; folderCount = d } }
                                catch (e: Exception) { error = "تعذر إكمال الفحص: ${e.localizedMessage ?: "تعذر الوصول إلى الملفات"}" }
                                finally { scanning = false }
                            }
                        }
                    },
                    enabled = !scanning,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal, contentColor = Color(0xFF06211E))
                ) {
                    if (scanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF06211E))
                        Spacer(Modifier.width(9.dp))
                        Text("جارٍ فحص الملفات…")
                    } else { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("بدء فحص حقيقي") }
                }
                AnimatedVisibility(scanning) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = teal)
                        Text("تم فحص ${fileCount} ملفاً و${folderCount} مجلداً…", color = muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                error?.let { Text(it, color = Color(0xFFFF9D91), style = MaterialTheme.typography.bodySmall) }
                result?.let { r ->
                    HorizontalDivider(color = muted.copy(alpha = .18f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("${r.files} ملف", teal, foreground)
                        Pill("${r.folders} مجلد", violet, foreground)
                    }
                    Text("الحجم الإجمالي: ${bytesLabel(r.bytes)}", color = foreground, fontWeight = FontWeight.SemiBold)
                    Text("ملفات مؤقتة مرشحة للمراجعة: ${r.junk.size}", color = if (r.junk.isEmpty()) teal else Color(0xFFFFC56D), fontWeight = FontWeight.SemiBold)
                    r.junk.take(8).forEach { Text("• $it", color = muted, style = MaterialTheme.typography.bodySmall) }
                    if (r.junk.size > 8) Text("و${r.junk.size - 8} عناصر أخرى", color = muted, style = MaterialTheme.typography.bodySmall)
                    if (r.largest.isNotEmpty()) {
                        Text("أكبر الملفات", color = foreground, fontWeight = FontWeight.SemiBold)
                        r.largest.forEach { (name, size) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, Modifier.weight(1f), maxLines = 1, color = muted, style = MaterialTheme.typography.bodySmall)
                                Text(bytesLabel(size), color = foreground, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, foreground: Color, muted: Color, accent: Color) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(accent.copy(alpha = .09f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Text(title, color = muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = foreground, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Pill(text: String, color: Color, foreground: Color) {
    Surface(color = color.copy(alpha = .14f), shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = foreground, style = MaterialTheme.typography.labelMedium)
    }
}
