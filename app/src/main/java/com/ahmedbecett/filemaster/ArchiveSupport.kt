package com.ahmedbecett.filemaster

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

private const val MAX_ARCHIVE_ENTRIES = 10_000
private const val MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L

private class ExtractionBudget {
    var entries = 0
    var bytes = 0L
    fun addEntry() {
        entries++
        if (entries > MAX_ARCHIVE_ENTRIES) throw IOException("الأرشيف يحتوي على عدد كبير جداً من العناصر.")
    }
    fun addBytes(count: Int) {
        bytes += count
        if (bytes > MAX_ARCHIVE_BYTES) throw IOException("تجاوز حجم الاستخراج الحد المسموح (512 ميغابايت).")
    }
}

private fun safeArchiveParts(raw: String): List<String> {
    val name = raw.replace('\\', '/')
    val parts = name.split('/').filter { it.isNotBlank() }
    if (name.startsWith("/") || name.startsWith("\\") || name.length > 4096 ||
        parts.any { it == "." || it == ".." || it.contains(':') }) {
        throw IOException("يحتوي الأرشيف على مسار غير آمن.")
    }
    return parts
}

private fun archiveTarget(root: DocumentFile, rawName: String, directory: Boolean): DocumentFile? {
    val parts = safeArchiveParts(rawName)
    if (parts.isEmpty()) return null
    var parent = root
    for (part in parts.dropLast(1)) {
        val found = parent.findFile(part)
        parent = when {
            found == null -> parent.createDirectory(part) ?: throw IOException("تعذر إنشاء مجلد: " + part)
            found.isDirectory -> found
            else -> throw IOException("يوجد ملف في مسار المجلد: " + part)
        }
    }
    val leaf = parts.last()
    val existing = parent.findFile(leaf)
    if (directory) {
        if (existing != null) {
            if (!existing.isDirectory) throw IOException("يوجد ملف بالاسم المطلوب للمجلد: " + leaf)
            return existing
        }
        return parent.createDirectory(leaf) ?: throw IOException("تعذر إنشاء مجلد: " + leaf)
    }
    if (existing?.isDirectory == true) throw IOException("يوجد مجلد بالاسم المطلوب للملف: " + leaf)
    val targetName = if (existing == null) leaf else {
        val dot = leaf.lastIndexOf('.')
        val stem = if (dot > 0) leaf.substring(0, dot) else leaf
        val ext = if (dot > 0) leaf.substring(dot) else ""
        var suffix = 1
        var candidate: String
        do {
            candidate = stem + "-extracted-" + suffix + ext
            suffix++
        } while (parent.findFile(candidate) != null && suffix < 10000)
        candidate
    }
    return parent.createFile("application/octet-stream", targetName)
        ?: throw IOException("تعذر إنشاء الملف: " + targetName)
}

private fun writeArchiveEntry(
    context: Context,
    root: DocumentFile,
    name: String,
    directory: Boolean,
    input: InputStream?,
    budget: ExtractionBudget
) {
    budget.addEntry()
    val target = archiveTarget(root, name, directory) ?: return
    if (directory) return
    val source = input ?: throw IOException("تعذرت قراءة محتويات الأرشيف.")
    context.contentResolver.openOutputStream(target.uri, "w")?.use { output ->
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val count = source.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            budget.addBytes(count)
            output.write(buffer, 0, count)
        }
        output.flush()
    } ?: throw IOException("تعذرت الكتابة إلى الملف: " + name)
}

private fun copyToTemp(context: Context, uri: Uri, suffix: String): File {
    val temp = File.createTempFile("filemaster-archive-", suffix, context.cacheDir)
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output, 32 * 1024) }
        } ?: throw IOException("تعذرت قراءة الأرشيف المحدد.")
        return temp
    } catch (e: Exception) {
        temp.delete()
        throw e
    }
}

fun extractArchiveSafely(context: Context, archiveUri: Uri, archiveName: String, destinationUri: Uri): String {
    val root = DocumentFile.fromTreeUri(context, destinationUri)
        ?: return "تعذر فتح مجلد الحفظ."
    val name = archiveName.lowercase()
    return try {
        when {
            name.endsWith(".zip") -> extractZipSafely(context, archiveUri, destinationUri)
            name.endsWith(".tar.gz") || name.endsWith(".tgz") -> extractTar(context, archiveUri, root, true)
            name.endsWith(".tar") -> extractTar(context, archiveUri, root, false)
            name.endsWith(".gz") -> extractGzipFile(context, archiveUri, root, archiveName)
            name.endsWith(".7z") -> extractSevenZip(context, archiveUri, root)
            name.endsWith(".rar") -> extractRar(context, archiveUri, root)
            else -> "صيغة الأرشيف غير مدعومة. الصيغ المدعومة: ZIP و7Z وRAR وTAR وGZIP."
        }
    } catch (e: Exception) {
        "فشل فك الضغط: " + (e.localizedMessage ?: "خطأ غير معروف")
    }
}

private fun extractTar(context: Context, uri: Uri, root: DocumentFile, gzip: Boolean): String {
    val budget = ExtractionBudget()
    val raw = context.contentResolver.openInputStream(uri) ?: throw IOException("تعذرت قراءة الأرشيف.")
    val base = if (gzip) GZIPInputStream(BufferedInputStream(raw)) else BufferedInputStream(raw)
    var files = 0
    TarArchiveInputStream(base).use { tar ->
        var entry = tar.nextTarEntry
        while (entry != null) {
            writeArchiveEntry(context, root, entry.name, entry.isDirectory, tar, budget)
            if (!entry.isDirectory) files++
            entry = tar.nextTarEntry
        }
    }
    return "تم استخراج " + files + " ملف بنجاح."
}

private fun extractGzipFile(context: Context, uri: Uri, root: DocumentFile, archiveName: String): String {
    val outputName = archiveName.substringBeforeLast('.', archiveName).ifBlank { "extracted-file" }
    val budget = ExtractionBudget()
    val raw = context.contentResolver.openInputStream(uri) ?: throw IOException("تعذرت قراءة الأرشيف.")
    GZIPInputStream(BufferedInputStream(raw)).use { gzip ->
        writeArchiveEntry(context, root, outputName, false, gzip, budget)
    }
    return "تم استخراج الملف: " + outputName
}

private fun extractSevenZip(context: Context, uri: Uri, root: DocumentFile): String {
    val temp = copyToTemp(context, uri, ".7z")
    val budget = ExtractionBudget()
    var files = 0
    try {
        SevenZFile.builder().setFile(temp).get().use { seven ->
            var entry = seven.nextEntry
            while (entry != null) {
                val stream = object : InputStream() {
                    override fun read(): Int {
                        val one = ByteArray(1)
                        val count = seven.read(one, 0, 1)
                        return if (count < 0) -1 else one[0].toInt() and 0xff
                    }
                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = seven.read(buffer, offset, length)
                }
                writeArchiveEntry(context, root, entry.name, entry.isDirectory, stream, budget)
                if (!entry.isDirectory) files++
                entry = seven.nextEntry
            }
        }
    } finally {
        temp.delete()
    }
    return "تم استخراج " + files + " ملف بنجاح."
}

private fun extractRar(context: Context, uri: Uri, root: DocumentFile): String {
    val temp = copyToTemp(context, uri, ".rar")
    val budget = ExtractionBudget()
    var files = 0
    try {
        Archive(temp).use { archive ->
            var header: FileHeader? = archive.nextFileHeader()
            while (header != null) {
                budget.addEntry()
                val entryName = header.fileName
                val directory = header.isDirectory
                val target = archiveTarget(root, entryName, directory)
                if (!directory && target != null) {
                    val counted = object : OutputStream() {
                        override fun write(value: Int) {
                            budget.addBytes(1)
                            context.contentResolver.openOutputStream(target.uri, "wa")?.use { it.write(value) }
                                ?: throw IOException("تعذرت الكتابة إلى الملف.")
                        }
                        override fun write(buffer: ByteArray, offset: Int, length: Int) {
                            budget.addBytes(length)
                            context.contentResolver.openOutputStream(target.uri, "wa")?.use { it.write(buffer, offset, length) }
                                ?: throw IOException("تعذرت الكتابة إلى الملف.")
                        }
                    }
                    counted.use { archive.extractFile(header, it) }
                    files++
                }
                header = archive.nextFileHeader()
            }
        }
    } finally {
        temp.delete()
    }
    return "تم استخراج " + files + " ملف بنجاح."
}
