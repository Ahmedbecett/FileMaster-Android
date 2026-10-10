package com.ahmedbecett.filemaster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
    var browseFolder by remember { mutableStateOf<DocumentFile?>(null) }
    var browseRootUri by remember { mutableStateOf<Uri?>(null) }
    var browseFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var zipSelection by remember { mutableStateOf(setOf<String>()) }
    var browsePage by remember { mutableIntStateOf(0) }
    var zipSources by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var managerDialog by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var moveOperation by remember { mutableStateOf(false) }

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

    val browsePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            browseRootUri = uri
            browseFolder = DocumentFile.fromTreeUri(context, uri)
            browsePage = 0
            browseFiles = browseFolder?.listFiles()?.sortedWith(
                compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }
            ) ?: emptyList()
            zipSelection = emptySet()
            status = "تم فتح مجلد الذاكرة. اضغط على مجلد للدخول أو حدد ملفات لإنشاء ZIP."
            success = true
        }
    }

    val operationDestinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (_: SecurityException) { }
            val target = DocumentFile.fromTreeUri(context, uri)
            val sources = browseFiles.filter { it.uri.toString() in zipSelection }
            if (target != null) {
                busy = true
                status = if (moveOperation) "جارٍ نقل العناصر…" else "جارٍ نسخ العناصر…"
                scope.launch {
                    val result = withContext(Dispatchers.IO) { copyOrMoveDocuments(context, sources, target, moveOperation) }
                    busy = false
                    status = result
                    browseFiles = browseFolder?.listFiles()?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                    zipSelection = emptySet()
                }
            }
        }
    }

    val zipCreator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && zipSources.isNotEmpty()) {
            busy = true
            status = "جارٍ إنشاء أرشيف ZIP…"
            val sources = zipSources
            scope.launch {
                val result = withContext(Dispatchers.IO) { createZipArchive(context, sources, uri) }
                busy = false
                success = result.startsWith("تم إنشاء")
                status = result
                zipSources = emptyList()
            }
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Accent)
                            Column(Modifier.weight(1f)) {
                                Text("مستكشف ذاكرة الهاتف", fontWeight = FontWeight.Bold)
                                Text(browseFolder?.name ?: "اختر مجلداً لعرض محتوياته", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { browsePicker.launch(null) }) { Text("تصفح") }
                        }
                        if (browseFolder != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    val root = browseFolder
                                    val parent = root?.parentFile
                                    if (parent != null) {
                                        browsePage = 0
                                        browseFolder = parent
                                        browseFiles = parent.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                        zipSelection = emptySet()
                                    }
                                }) { Text("المجلد السابق") }
                                OutlinedButton(onClick = {
                                    destination = browseRootUri
                                    status = "تم تعيين المجلد الرئيسي المختار كوجهة لفك الضغط."
                                    success = true
                                }) { Text("استخدام كوجهة") }
                            }
                            if (browseFiles.isEmpty()) {
                                Text("المجلد فارغ", color = Muted, style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { managerName = ""; managerDialog = "mkdir" }, modifier = Modifier.fillMaxWidth()) { Text("إنشاء مجلد هنا") }
                            } else {
                                browseFiles.drop(browsePage * 8).take(8).forEach { file ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            if (file.isDirectory) {
                                                browsePage = 0
                                                browseFolder = file
                                                browseFiles = file.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                                zipSelection = emptySet()
                                            } else {
                                                selectedZip = file.uri
                                                selectedName = file.name
                                                status = "تم اختيار " + (file.name ?: "الملف") + ". إذا كان ZIP يمكنك فك ضغطه."
                                                success = true
                                            }
                                        }.padding(vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (file.isDirectory) Icons.Default.FolderOpen else Icons.Default.InsertDriveFile, contentDescription = null, tint = Accent)
                                        Text(file.name ?: "بدون اسم", modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, color = Color(0xFFE0E6EF))
                                        Checkbox(
                                            checked = file.uri.toString() in zipSelection,
                                            onCheckedChange = { checked ->
                                                zipSelection = if (checked) zipSelection + file.uri.toString() else zipSelection - file.uri.toString()
                                            }
                                        )
                                    }
                                }
                                if (browseFiles.size > 8) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton(enabled = browsePage > 0, onClick = { browsePage-- }) { Text("السابق") }
                                        Text((browsePage + 1).toString() + " / " + ((browseFiles.size + 7) / 8).toString(), color = Muted)
                                        TextButton(enabled = (browsePage + 1) * 8 < browseFiles.size, onClick = { browsePage++ }) { Text("التالي") }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { managerName = ""; managerDialog = "mkdir" }, modifier = Modifier.weight(1f)) { Text("مجلد جديد") }
                                    OutlinedButton(onClick = {
                                        if (zipSelection.size == 1) {
                                            managerName = browseFiles.firstOrNull { it.uri.toString() in zipSelection }?.name.orEmpty()
                                            managerDialog = "rename"
                                        } else status = "حدد عنصراً واحداً لإعادة تسميته."
                                    }, modifier = Modifier.weight(1f)) { Text("إعادة تسمية") }
                                    OutlinedButton(onClick = { managerDialog = "delete" }, modifier = Modifier.weight(1f)) { Text("حذف") }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { moveOperation = false; operationDestinationPicker.launch(null) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, null); Text("نسخ") }
                                    OutlinedButton(onClick = { moveOperation = true; operationDestinationPicker.launch(null) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DriveFileMove, null); Text("نقل") }
                                }
                                OutlinedButton(
                                    onClick = {
                                        zipSources = browseFiles.filter { it.uri.toString() in zipSelection }
                                        if (zipSources.isEmpty()) status = "حدد ملفاً واحداً على الأقل لإنشاء ZIP."
                                        else zipCreator.launch("FileMaster-archive.zip")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("إنشاء ZIP من العناصر المحددة (" + zipSelection.size + ")") }
                            }
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
        if (managerDialog == "mkdir" || managerDialog == "rename") {
            AlertDialog(
                onDismissRequest = { managerDialog = "" },
                title = { Text(if (managerDialog == "mkdir") "إنشاء مجلد" else "إعادة تسمية") },
                text = { OutlinedTextField(value = managerName, onValueChange = { managerName = it }, label = { Text("الاسم") }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        val safeName = managerName.trim()
                        if (safeName.isBlank() || safeName == "." || safeName == ".." || safeName.contains("/") || safeName.contains(":")) {
                            status = "أدخل اسماً صالحاً."
                        } else {
                            val selectedFile = browseFiles.firstOrNull { it.uri.toString() in zipSelection }
                            val ok = if (managerDialog == "mkdir") browseFolder?.findFile(safeName) == null && browseFolder?.createDirectory(safeName) != null else selectedFile?.renameTo(safeName) == true
                            status = if (ok) "تمت العملية بنجاح." else "تعذرت العملية؛ تحقق من الصلاحيات والاسم."
                            managerDialog = ""
                            browseFiles = browseFolder?.listFiles()?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                            zipSelection = emptySet()
                        }
                    }) { Text("تنفيذ") }
                },
                dismissButton = { TextButton(onClick = { managerDialog = "" }) { Text("إلغاء") } }
            )
        }
        if (managerDialog == "delete") {
            AlertDialog(
                onDismissRequest = { managerDialog = "" },
                title = { Text("حذف العناصر المحددة؟") },
                text = { Text("سيتم حذف " + zipSelection.size + " عنصر. قد لا يمكن التراجع عن ذلك.") },
                confirmButton = {
                    TextButton(onClick = {
                        val targets = browseFiles.filter { it.uri.toString() in zipSelection }
                        val removed = targets.count { it.delete() }
                        status = "تم حذف " + removed + " من " + targets.size + " عنصر."
                        managerDialog = ""
                        zipSelection = emptySet()
                        browseFiles = browseFolder?.listFiles()?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                    }) { Text("حذف") }
                },
                dismissButton = { TextButton(onClick = { managerDialog = "" }) { Text("إلغاء") } }
            )
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

private fun createZipArchive(context: android.content.Context, sources: List<DocumentFile>, uri: Uri): String {
    var count = 0
    return try {
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: return "تعذر إنشاء ملف ZIP."
        output.use { raw ->
            ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                for (source in sources) count += addDocumentToZip(context, zip, source, source.name ?: "file")
            }
        }
        "تم إنشاء أرشيف ZIP بنجاح، عدد الملفات: " + count
    } catch (e: Exception) {
        "فشل إنشاء ZIP: " + (e.localizedMessage ?: "خطأ غير معروف")
    }
}

private fun addDocumentToZip(context: android.content.Context, zip: ZipOutputStream, source: DocumentFile, path: String): Int {
    val safePath = path.trimStart('/')
    if (source.isDirectory) {
        zip.putNextEntry(ZipEntry(safePath + "/"))
        zip.closeEntry()
        var count = 0
        for (child in source.listFiles()) count += addDocumentToZip(context, zip, child, safePath + "/" + (child.name ?: "file"))
        return count
    }
    zip.putNextEntry(ZipEntry(safePath))
    context.contentResolver.openInputStream(source.uri)?.use { input -> input.copyTo(zip, 32768) }
        ?: throw IllegalStateException("تعذرت قراءة الملف: " + path)
    zip.closeEntry()
    return 1
}

private fun copyOrMoveDocuments(context: android.content.Context, sources: List<DocumentFile>, destination: DocumentFile, move: Boolean): String {
    var completed = 0
    return try {
        for (source in sources) {
            if (source.uri == destination.uri || destination.uri.toString().startsWith(source.uri.toString())) {
                throw IllegalStateException("لا يمكن نسخ مجلد إلى نفسه أو إلى مجلد داخله.")
            }
            copyDocumentTree(context, source, destination)
            if (move && !source.delete()) throw IllegalStateException("تم النسخ لكن تعذر حذف المصدر: " + source.name)
            completed++
        }
        "اكتملت العملية: " + completed + " عنصر."
    } catch (e: Exception) {
        "توقفت العملية بعد " + completed + " عنصر: " + (e.localizedMessage ?: "خطأ غير معروف")
    }
}

private fun copyDocumentTree(context: android.content.Context, source: DocumentFile, parent: DocumentFile) {
    val name = source.name ?: "file"
    if (source.isDirectory) {
        val existing = parent.findFile(name)
        val target = if (existing?.isDirectory == true) existing else parent.createDirectory(name)
            ?: throw IllegalStateException("تعذر إنشاء المجلد: " + name)
        for (child in source.listFiles()) copyDocumentTree(context, child, target)
    } else {
        val dot = name.lastIndexOf('.')
        val targetName = if (parent.findFile(name) == null) name else if (dot > 0) name.substring(0, dot) + "-copy" + name.substring(dot) else name + "-copy"
        val target = parent.createFile("application/octet-stream", targetName) ?: throw IllegalStateException("تعذر إنشاء الملف: " + targetName)
        context.contentResolver.openInputStream(source.uri)?.use { input ->
            context.contentResolver.openOutputStream(target.uri, "w")?.use { output -> input.copyTo(output, 32768) }
                ?: throw IllegalStateException("تعذرت الكتابة إلى الملف.")
        } ?: throw IllegalStateException("تعذرت قراءة الملف.")
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
