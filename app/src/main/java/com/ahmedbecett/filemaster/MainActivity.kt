package com.ahmedbecett.filemaster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

private val Ink = Color(0xFF101522)
private val Panel = Color(0xFF1B2434)
private val Accent = Color(0xFF79E0C3)
private val Muted = Color(0xFFADB8C8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FileMasterApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileMasterApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedZip by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var destination by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("اختر أرشيف ZIP للبدء.") }
    var success by remember { mutableStateOf(false) }

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedZip = uri
            selectedName = DocumentFile.fromSingleUri(context, uri)?.name ?: "archive.zip"
            success = false
            status = "تم اختيار الأرشيف. اختر مجلد الحفظ لفك الضغط."
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            destination = uri
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            status = "تم اختيار مجلد الحفظ. اضغط «فك الضغط الآن»."
            success = false
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, onPrimary = Ink, background = Ink, surface = Panel,
        onSurface = Color(0xFFF4F7FB), secondary = Muted
    )) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("FileMaster", fontWeight = FontWeight.Bold)
                            Text("مدير الملفات والأرشيفات", style = MaterialTheme.typography.labelMedium, color = Muted)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Panel)
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(58.dp).background(Accent.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
                        }
                        Text("ملفاتك، تحت سيطرتك", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "تصفح ملفات الهاتف واختر الأرشيف ومجلد الاستخراج. تتم عمليات الملفات على جهازك.",
                            style = MaterialTheme.typography.bodyMedium, color = Muted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Accent)
                            Text("وصول آمن إلى التخزين عبر Android", style = MaterialTheme.typography.labelMedium, color = Accent)
                        }
                    }
                }

                Text("أدوات سريعة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ActionCard(
                    icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Accent) },
                    title = "اختيار أرشيف ZIP",
                    detail = selectedName ?: "اختر ملفاً من ذاكرة الهاتف",
                    buttonText = "تصفح الملفات",
                    onClick = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")) }
                )
                ActionCard(
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Accent) },
                    title = "مجلد الاستخراج",
                    detail = if (destination == null) "اختر المكان الذي تريد حفظ الملفات فيه" else "تم تحديد مجلد الحفظ",
                    buttonText = "اختيار المجلد",
                    onClick = { folderPicker.launch(null) }
                )

                Button(
                    onClick = {
                        val archive = selectedZip
                        val target = destination
                        if (archive == null) {
                            status = "اختر ملف ZIP أولاً."
                            success = false
                        } else if (target == null) {
                            status = "اختر مجلد الحفظ أولاً."
                            success = false
                        } else {
                            busy = true
                            success = false
                            status = "جارٍ فك الضغط…"
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { extractZipSafely(context, archive, target) }
                                busy = false
                                success = result.startsWith("تم استخراج")
                                status = result
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                    }
                    Text(if (busy) "جارٍ الاستخراج…" else "فك الضغط الآن", fontWeight = FontWeight.Bold)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (success) Icons.Default.CheckCircle else Icons.Default.Archive,
                            contentDescription = null, tint = if (success) Accent else Muted
                        )
                        Text(status, color = if (success) Accent else Color(0xFFE0E6EF), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "النسخة 1.0 • صُمّم بواسطة Ahmedbecett",
                    modifier = Modifier.fillMaxWidth(), color = Muted, style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: @Composable () -> Unit,
    title: String,
    detail: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(Accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            OutlinedButton(onClick = onClick, shape = RoundedCornerShape(14.dp)) { Text(buttonText) }
        }
    }
}

private fun extractZipSafely(context: android.content.Context, archiveUri: Uri, destinationUri: Uri): String {
    var extracted = 0
    return try {
        val root = DocumentFile.fromTreeUri(context, destinationUri) ?: return "تعذر فتح مجلد الحفظ."
        val input = context.contentResolver.openInputStream(archiveUri) ?: return "تعذر قراءة الأرشيف المحدد."
        input.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val rawName = entry.name.replace('\\', '/')
                    val parts = rawName.split('/').filter { it.isNotBlank() }
                    if (rawName.startsWith("/") || parts.any { it == ".." || it == "." }) {
                        return "توقف الاستخراج: يحتوي الأرشيف على مسار غير آمن."
                    }
                    if (parts.isNotEmpty()) {
                        var parent = root
                        for (part in parts.dropLast(1)) {
                            parent = parent.findFile(part) ?: parent.createDirectory(part)
                                ?: throw IllegalStateException("تعذر إنشاء مجلد: " + part)
                        }
                        val leaf = parts.last()
                        if (entry.isDirectory) {
                            if (parent.findFile(leaf) == null && parent.createDirectory(leaf) == null) {
                                throw IllegalStateException("تعذر إنشاء المجلد: " + leaf)
                            }
                        } else {
                            val existing = parent.findFile(leaf)
                            if (existing != null && !existing.delete()) {
                                throw IllegalStateException("تعذر استبدال الملف: " + leaf)
                            }
                            val outputFile = parent.createFile("application/octet-stream", leaf)
                                ?: throw IllegalStateException("تعذر إنشاء الملف: " + leaf)
                            context.contentResolver.openOutputStream(outputFile.uri, "w")?.use { output ->
                                val buffer = ByteArray(32 * 1024)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    output.write(buffer, 0, count)
                                }
                                output.flush()
                            } ?: throw IllegalStateException("تعذر الكتابة إلى الملف: " + leaf)
                            extracted++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        "تم استخراج " + extracted + " ملف بنجاح."
    } catch (e: Exception) {
        "فشل فك الضغط: " + (e.localizedMessage ?: "خطأ غير معروف")
    }
}
