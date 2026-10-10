package com.ahmedbecett.filemaster

import android.content.Intent
import android.content.ActivityNotFoundException
import android.webkit.MimeTypeMap
import android.net.Uri
import android.os.Bundle
import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private var Ink by mutableStateOf(Color(0xFF0B1020))
private var Panel by mutableStateOf(Color(0xFF151F38))
private var Accent by mutableStateOf(Color(0xFF8A7CFF))
private var Muted by mutableStateOf(Color(0xFFA9B5D0))

private fun setFileMasterPalette(dark: Boolean) {
    if (dark) {
        Ink = Color(0xFF0A111F)
        Panel = Color(0xFF142033)
        Accent = Color(0xFF39D7C3)
        Muted = Color(0xFFA9B7C8)
    } else {
        Ink = Color(0xFFF3F6FA)
        Panel = Color(0xFFFFFFFF)
        Accent = Color(0xFF087F78)
        Muted = Color(0xFF536579)
    }
}

private suspend fun measureCpuUsagePercent(): Double? {
    fun snapshot(): Pair<Long, Long>? {
        val fields = try {
            java.io.File("/proc/stat").bufferedReader().use { it.readLine() }
                .trim().split(Regex("\\s+"))
        } catch (_: Exception) { return null }
        if (fields.firstOrNull() != "cpu") return null
        val ticks = fields.drop(1).mapNotNull { it.toLongOrNull() }
        if (ticks.size < 4) return null
        val total = ticks.sum()
        val idle = ticks.getOrElse(3) { 0L } + ticks.getOrElse(4) { 0L }
        return total to idle
    }
    val first = snapshot() ?: return null
    kotlinx.coroutines.delay(450)
    val second = snapshot() ?: return null
    val totalDelta = second.first - first.first
    val idleDelta = second.second - first.second
    if (totalDelta <= 0) return null
    return (((totalDelta - idleDelta).toDouble() / totalDelta) * 100.0).coerceIn(0.0, 100.0)
}

private fun measureInternetDownloadMbps(): Double? {
    var connection: java.net.HttpURLConnection? = null
    return try {
        connection = java.net.URL("https://speed.cloudflare.com/__down?bytes=1000000")
            .openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.requestMethod = "GET"
        connection.useCaches = false
        val start = System.nanoTime()
        var bytes = 0L
        connection.inputStream.use { input ->
            val buffer = ByteArray(8192)
            while (bytes < 1_000_000L) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), 1_000_000L - bytes).toInt())
                if (read <= 0) break
                bytes += read
            }
        }
        val seconds = ((System.nanoTime() - start).coerceAtLeast(1L)).toDouble() / 1_000_000_000.0
        if (bytes == 0L || seconds <= 0.0) null else (bytes * 8.0 / seconds / 1_000_000.0)
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val consentParameters = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            this,
            consentParameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    MobileAds.initialize(this)
                }
            },
            {
                // Ads SDK still initializes if the consent-info request fails; serving remains subject to Google policy.
                MobileAds.initialize(this)
            }
        )
        setContent { FileMasterApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private val FileMasterTranslations: Map<String, List<String>> = mapOf(
    "مدير الملفات والأرشيفات" to listOf("File & archive manager", "Gestionnaire de fichiers et d’archives", "ⴰⵎⵙⵙⵓⴷⵙ ⵏ ⵉⴼⴰⵢⵍⵓⵜⵏ"),
    "ملفاتك، تحت سيطرتك" to listOf("Your files, your control", "Vos fichiers, votre contrôle", "ⵉⴼⴰⵢⵍⵓⵜⵏⵏⴽ"),
    "تصفح ملفات الهاتف واختر الأرشيف ومجلد الاستخراج. تتم عمليات الملفات على جهازك." to listOf("Browse phone files and choose an archive and extraction folder. Files stay on your device.", "Parcourez les fichiers du téléphone et choisissez une archive et un dossier d’extraction. Les fichiers restent sur votre appareil.", "ⵙⵏⵉⵔⵉ ⵉⴼⴰⵢⵍⵓⵜⵏ ⵏ ⵜⵉⵍⵉⴼⵓⵏⵜ."),
    "وصول آمن إلى التخزين عبر Android" to listOf("Secure storage access through Android", "Accès sécurisé au stockage via Android", "ⴰⵎⵏⵏⵓ ⵖⵔ ⵓⵙⵍⴽⵉⵎ"),
    "مستكشف ذاكرة الهاتف" to listOf("Phone storage explorer", "Explorateur du stockage", "ⴰⵙⵏⵉⵔⵉ ⵏ ⵓⵙⵍⴽⵉⵎ"),
    "اختر مجلداً لعرض محتوياته" to listOf("Choose a folder to view its contents", "Choisissez un dossier pour afficher son contenu", "ⴼⵔⵏ ⴰⴽⴰⵔⵏ"),
    "تصفح" to listOf("Browse", "Parcourir", "ⵙⵏⵉⵔⵉ"),
    "المجلد السابق" to listOf("Parent folder", "Dossier parent", "ⴰⴽⴰⵔⵏ ⴰⵎⵣⵡⴰⵔⵓ"),
    "استخدام كوجهة" to listOf("Use as destination", "Utiliser comme destination", "ⵙⵙⵎⵔ ⴰⵎ ⴰⴽⴰⵔⵏ"),
    "ابحث في هذا المجلد" to listOf("Search this folder", "Rechercher dans ce dossier", "ⵔⵣⵓ ⴳ ⵓⴽⴰⵔⵏ"),
    "المجلد فارغ" to listOf("Folder is empty", "Le dossier est vide", "ⴰⴽⴰⵔⵏ ⵉⵍⵍⴰ"),
    "لا توجد نتائج مطابقة" to listOf("No matching results", "Aucun résultat correspondant", "ⵓⵔ ⵍⵍⵉⵏⵜ ⵉⴳⵎⴰⵎⵏ"),
    "إنشاء مجلد هنا" to listOf("Create folder here", "Créer un dossier ici", "ⵙⵏⵓⵍⴼⵓ ⴰⴽⴰⵔⵏ"),
    "السابق" to listOf("Previous", "Précédent", "ⵓⵣⵡⴰⵔ"),
    "التالي" to listOf("Next", "Suivant", "ⴷⴼⴼⵉⵔ"),
    "مجلد جديد" to listOf("New folder", "Nouveau dossier", "ⴰⴽⴰⵔⵏ ⴰⵎⴰⵢⵏⵓ"),
    "إعادة تسمية" to listOf("Rename", "Renommer", "ⵙⵏⴼⵍ ⵉⵙⵎ"),
    "حذف" to listOf("Delete", "Supprimer", "ⴽⴽⵙ"),
    "نسخ" to listOf("Copy", "Copier", "ⵙⵏⵖⵍ"),
    "نقل" to listOf("Move", "Déplacer", "ⵙⵉⵡⴹ"),
    "أدوات سريعة" to listOf("Quick tools", "Outils rapides", "ⵉⵎⵙⵙⵏⴼⴰⵍ"),
    "اختيار أرشيف ZIP" to listOf("Choose ZIP archive", "Choisir une archive ZIP", "ⴼⵔⵏ ⴰⵔⵛⵉⴼ ZIP"),
    "اختر ملفاً من ذاكرة الهاتف" to listOf("Choose a file from phone storage", "Choisissez un fichier dans le stockage", "ⴼⵔⵏ ⴰⴼⴰⵢⵍⵓ"),
    "تصفح الملفات" to listOf("Browse files", "Parcourir les fichiers", "ⵙⵏⵉⵔⵉ ⵉⴼⴰⵢⵍⵓⵜⵏ"),
    "مجلد الاستخراج" to listOf("Extraction folder", "Dossier d’extraction", "ⴰⴽⴰⵔⵏ ⵏ ⵓⵙⵎⵓⵔⵔⵉ"),
    "اختر المكان الذي تريد حفظ الملفات فيه" to listOf("Choose where to save files", "Choisissez où enregistrer les fichiers", "ⴼⵔⵏ ⴰⵏⵉⵡⴰⵍ ⵏ ⵓⵙⴽⵍⵙ"),
    "تم تحديد مجلد الحفظ" to listOf("Destination folder selected", "Dossier de destination sélectionné", "ⵜⵜⵓⴼⵔⵏ ⴰⴽⴰⵔⵏ"),
    "اختيار المجلد" to listOf("Choose folder", "Choisir un dossier", "ⴼⵔⵏ ⴰⴽⴰⵔⵏ"),
    "اختر ملف ZIP أولاً." to listOf("Choose a ZIP file first.", "Choisissez d’abord un fichier ZIP.", "ⴼⵔⵏ ⴰⴼⴰⵢⵍⵓ ZIP ⴷⴰⵜ."),
    "اختر مجلد الحفظ أولاً." to listOf("Choose a destination folder first.", "Choisissez d’abord un dossier de destination.", "ⴼⵔⵏ ⴰⴽⴰⵔⵏ ⴷⴰⵜ."),
    "فك الضغط الآن" to listOf("Extract now", "Extraire maintenant", "ⵙⵎⵓⵔⵔⵉ ⵜⵓⵔⴰ"),
    "الاسم" to listOf("Name", "Nom", "ⵉⵙⵎ"),
    "أدخل اسماً صالحاً." to listOf("Enter a valid name.", "Saisissez un nom valide.", "ⵙⵙⵉⵡⴹ ⵉⵙⵎ ⵉⵎⵙⴰⵡⴰⵏ."),
    "تنفيذ" to listOf("Apply", "Appliquer", "ⵙⵙⵎⵔ"),
    "إلغاء" to listOf("Cancel", "Annuler", "ⵙⵔ"),
    "اللغة" to listOf("Language", "Langue", "ⵜⵓⵜⵍⴰⵢⵜ")
)

private val FileMasterExtraTranslations: Map<String, List<String>> = mapOf(
    "سلة المحذوفات" to listOf("Trash", "Corbeille", "ⴰⴽⴰⵔⵏ ⵏ ⵉⵜⵜⵓⴽⴽⵙⵏ"),
    "العودة من سلة المحذوفات" to listOf("Back from Trash", "Retour de la corbeille", "ⵓⵖⴰⵍ ⵙⴳ ⵜⴽⵓⵔⴱⵉⵍⵜ"),
    "استعادة العناصر المحددة" to listOf("Restore selected items", "Restaurer les éléments sélectionnés", "ⵙⵎⵓⵔⵔⵉ ⵉⵙⵖⵓⵏ ⵉⵜⵜⵓⴼⵔⵏ"),
    "محتويات المجلد" to listOf("Folder contents", "Contenu du dossier", "ⵉⴳⴱⵔⴰⵏ ⵏ ⵓⴽⴰⵔⵏ"),
    "ستنقل العناصر المحددة إلى سلة المحذوفات ويمكن استعادتها لاحقاً." to listOf("Selected items will move to Trash and can be restored later.", "Les éléments seront déplacés vers la corbeille et pourront être restaurés.", "ⴰⴷ ⵜⵜⵓⵙⵉⵡⴹⵏ ⵉⵙⵖⵓⵏ ⵙ ⵜⴽⵓⵔⴱⵉⵍⵜ."),
    "سيتم حذف العناصر المحددة نهائياً." to listOf("Selected items will be permanently deleted.", "Les éléments sélectionnés seront supprimés définitivement.", "ⴰⴷ ⵜⵜⵓⴽⴽⵙⵏ ⵉⵙⵖⵓⵏ ⴰⴽⴽⵯ."),
    "تعذر إنشاء سلة المحذوفات؛ لم تُحذف الملفات." to listOf("Could not create Trash; no files were deleted.", "Impossible de créer la corbeille ; aucun fichier n’a été supprimé.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵙⵏⵓⵍⴼⵓ ⵜⴰⴽⵓⵔⴱⵉⵍⵜ."),
    "سلة المحذوفات: حدد العناصر لاستعادتها." to listOf("Trash: select items to restore.", "Corbeille : sélectionnez les éléments à restaurer.", "ⵜⴰⴽⵓⵔⴱⵉⵍⵜ: ⴼⵔⵏ ⵉⵙⵖⵓⵏ."),
    "الملفات المخفية" to listOf("Hidden files", "Fichiers masqués", "ⵉⴼⴰⵢⵍⵓⵜⵏ ⵉⵜⵜⵓⵙⵓⴼⵔⵏ"),
    "العودة من الملفات المخفية" to listOf("Back from hidden files", "Retour des fichiers masqués", "ⵓⵖⴰⵍ ⵙⴳ ⵉⴼⴰⵢⵍⵓⵜⵏ"),
    "إخفاء المحدد" to listOf("Hide selected", "Masquer la sélection", "ⵙⵓⴼⵔ ⵉⵜⵜⵓⴼⵔⵏ"),
    "إظهار العناصر المحددة" to listOf("Unhide selected items", "Afficher les éléments sélectionnés", "ⵙⵙⴽⵏ ⵉⵙⵖⵓⵏ"),
    "الملفات المخفية: حدد العناصر ثم أظهرها لاستعادتها." to listOf("Hidden files: select items and unhide them to restore.", "Fichiers masqués : sélectionnez puis affichez pour restaurer.", "ⵉⴼⴰⵢⵍⵓⵜⵏ ⵉⵜⵜⵓⵙⵓⴼⵔⵏ."),
    "إعداد قفل الخصوصية" to listOf("Set privacy lock", "Configurer le verrou de confidentialité", "ⵙⵏⵓⵍⴼⵓ ⴰⵎⵙⴰⵔ ⵏ ⵜⴱⴰⴹⵉⵜ"),
    "قفل الخصوصية" to listOf("Privacy lock", "Verrou de confidentialité", "ⴰⵎⵙⴰⵔ ⵏ ⵜⴱⴰⴹⵉⵜ"),
    "أدخل رمز الخصوصية لفتح ملفاتك" to listOf("Enter your privacy PIN to unlock FileMaster", "Saisissez votre code pour déverrouiller FileMaster", "ⵙⵙⵉⵡⴹ ⴰⵏⴰⵍ ⵏ ⵜⴱⴰⴹⵉⵜ"),
    "رمز PIN" to listOf("PIN", "Code PIN", "PIN"),
    "فتح التطبيق" to listOf("Unlock app", "Déverrouiller", "ⵍⴷⵉ ⴰⵙⵏⴰⵙ"),
    "إزالة القفل باستخدام الرمز" to listOf("Remove lock using PIN", "Supprimer le verrou avec le code", "ⴽⴽⵙ ⴰⵎⵙⴰⵔ"),
    "الرمز غير صحيح." to listOf("Incorrect PIN.", "Code incorrect.", "ⴰⵏⴰⵍ ⵓⵔ ⵉⵎⵙⴰⵡⴰⵏ."),
    "رمز PIN جديد" to listOf("New PIN", "Nouveau code PIN", "PIN ⴰⵎⴰⵢⵏⵓ"),
    "حفظ الرمز" to listOf("Save PIN", "Enregistrer le code", "ⵙⴽⵍⵙ ⴰⵏⴰⵍ"),
    "إغلاق المعاينة" to listOf("Close preview", "Fermer l’aperçu", "ⵎⴷⵍ ⵜⴰⵙⵎⵓⵔⵔⵉ"),
    "تعذرت معاينة الصورة." to listOf("Could not preview image.", "Impossible d’afficher l’aperçu.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵙⵎⵓⵔⵔⵉ ⵜⴰⵡⵍⴰⴼⵜ"),
    "اختر أرشيفاً أولاً." to listOf("Choose an archive first.", "Choisissez d’abord une archive.", "ⴼⵔⵏ ⴰⵔⵛⵉⴼ ⴷⴰⵜ"),
    "اختيار أرشيف" to listOf("Choose archive", "Choisir une archive", "ⴼⵔⵏ ⴰⵔⵛⵉⴼ"),
    "معاينة محتويات الأرشيف" to listOf("Preview archive contents", "Aperçu du contenu de l’archive", "ⵙⵎⵓⵔⵔⵉ ⵉⴳⴱⵔⴰⵏ ⵏ ⵜⴰⵔⵛⵉⴼⵜ"),
    "اختر أرشيف ZIP للبدء." to listOf("Choose a ZIP archive to begin.", "Choisissez une archive ZIP pour commencer.", "ⴼⵔⵏ ⴰⵔⵛⵉⴼ ZIP ⵉ ⵓⴱⴷⵓ."),
    "تم اختيار الأرشيف. اختر مجلد الحفظ لفك الضغط." to listOf("Archive selected. Choose a destination folder.", "Archive sélectionnée. Choisissez un dossier de destination.", "ⵜⵜⵓⴼⵔⵏ ⴰⵔⵛⵉⴼ. ⴼⵔⵏ ⴰⴽⴰⵔⵏ."),
    "تم اختيار مجلد الحفظ. اضغط «فك الضغط الآن»." to listOf("Destination selected. Tap “Extract now”.", "Destination sélectionnée. Appuyez sur « Extraire maintenant ».", "ⵜⵜⵓⴼⵔⵏ ⴰⴽⴰⵔⵏ. ⵙⵎⵓⵔⵔⵉ ⵜⵓⵔⴰ."),
    "تم فتح مجلد الذاكرة. اضغط على مجلد للدخول أو حدد ملفات لإنشاء ZIP." to listOf("Folder opened. Open a folder or select files to create a ZIP.", "Dossier ouvert. Ouvrez un dossier ou sélectionnez des fichiers pour créer un ZIP.", "ⵍⵍⴰ ⵓⴽⴰⵔⵏ. ⴼⵔⵏ ⵉⴼⴰⵢⵍⵓⵜⵏ ⵉ ZIP."),
    "جارٍ نقل العناصر…" to listOf("Moving items…", "Déplacement des éléments…", "ⴰⵙⵉⵡⴹ…"),
    "جارٍ نسخ العناصر…" to listOf("Copying items…", "Copie des éléments…", "ⴰⵙⵏⵖⵍ…"),
    "جارٍ إنشاء أرشيف ZIP…" to listOf("Creating ZIP archive…", "Création de l’archive ZIP…", "ⴰⵙⵏⵓⵍⴼⵓ ⵏ ZIP…"),
    "تم اختيار " to listOf("Selected: ", "Sélectionné : ", "ⵜⵜⵓⴼⵔⵏ: "),
    ". استخدم زر فك الضغط لاستخراجه." to listOf(". Use Extract to unpack it.", ". Utilisez Extraire pour le décompresser.", ". ⵙⵙⵎⵔ ⵙⵎⵓⵔⵔⵉ."),
    "تم فتح " to listOf("Opened: ", "Ouvert : ", "ⵍⵍⴰ: "),
    " باستخدام التطبيق المناسب." to listOf(" with the appropriate app.", " avec l’application appropriée.", " ⵙ ⵓⵙⵏⴰⵙ ⵉⵎⵙⴰⵡⴰⵏ."),
    "تعذر فتح الملف: " to listOf("Could not open file: ", "Impossible d’ouvrir le fichier : ", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵍⴷⵉ ⴰⴼⴰⵢⵍⵓ: "),
    "حدد عنصراً واحداً لإعادة تسميته." to listOf("Select one item to rename.", "Sélectionnez un élément à renommer.", "ⴼⵔⵏ ⴰⵙⵖⵓⵏ ⵢⴰⵏ."),
    "حدد ملفاً واحداً على الأقل لإنشاء ZIP." to listOf("Select at least one item to create a ZIP.", "Sélectionnez au moins un élément pour créer un ZIP.", "ⴼⵔⵏ ⴰⵙⵖⵓⵏ ⵉ ZIP."),
    "إنشاء ZIP من العناصر المحددة (" to listOf("Create ZIP from selected items (", "Créer un ZIP à partir des éléments sélectionnés (", "ⵙⵏⵓⵍⴼⵓ ZIP ("),
    "تم تعيين المجلد الرئيسي المختار كوجهة لفك الضغط." to listOf("Selected root folder set as extraction destination.", "Le dossier racine sélectionné est défini comme destination d’extraction.", "ⴰⴽⴰⵔⵏ ⵉⵜⵜⵓⴼⵔⵏ ⵏ ⵓⵙⵎⵓⵔⵔⵉ."),
    "لا يوجد تطبيق مثبت لفتح هذا النوع من الملفات." to listOf("No installed app can open this file type.", "Aucune application installée ne peut ouvrir ce type de fichier.", "ⵓⵔ ⵍⵍⵉ ⴰⵙⵏⴰⵙ ⵉ ⵓⴼⴰⵢⵍⵓ."),
    "خطأ غير معروف" to listOf("Unknown error", "Erreur inconnue", "ⴰⵣⴳⵍ ⵓⵔ ⵉⵜⵜⵓⵙⵙⵏ"),
    "بدون اسم" to listOf("Unnamed", "Sans nom", "ⵡⴰⵔ ⵉⵙⵎ"),
    "النسخة 1.0 • صُمّم بواسطة Ahmedbecett" to listOf("Version 1.0 • Designed by Ahmedbecett", "Version 1.0 • Conçu par Ahmedbecett", "ⵍⵇⵇⵎ 1.0 • Ahmedbecett"),
    "تمت العملية بنجاح." to listOf("Operation completed successfully.", "Opération terminée avec succès.", "ⵜⴰⵎⵙⴰⵙⵜ ⵜⴼⴰⴽ."),
    "تعذرت العملية؛ تحقق من الصلاحيات والاسم." to listOf("Operation failed; check permissions and name.", "Échec de l’opération ; vérifiez les autorisations et le nom.", "ⵜⴰⵎⵙⴰⵙⵜ ⵜⵏⵣⴳⵍ."),
    "حذف العناصر المحددة؟" to listOf("Delete selected items?", "Supprimer les éléments sélectionnés ?", "ⴽⴽⵙ ⵉⵙⵖⵓⵏ ⵉⵜⵜⵓⴼⵔⵏ?"),
    "سيتم حذف " to listOf("Will delete ", "Suppression de ", "ⴰⴷ ⵉⵜⵜⵓⴽⴽⵙ "),
    " عنصر. قد لا يمكن التراجع عن ذلك." to listOf(" item(s). This may not be reversible.", " élément(s). Cette action peut être irréversible.", " ⵉⵙⵖⵓⵏ. ⵓⵔ ⵉⵣⵎⵉⵔ ⵓⵙⵓⵖⵍ."),
    "تم حذف " to listOf("Deleted ", "Supprimé ", "ⵉⵜⵜⵓⴽⴽⵙ "),
    " من " to listOf(" of ", " sur ", " ⵙⴳ "),
    " عنصر." to listOf(" item(s).", " élément(s).", " ⵉⵙⵖⵓⵏ."),
    "تعذر إنشاء ملف ZIP." to listOf("Could not create ZIP file.", "Impossible de créer le fichier ZIP.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵙⵏⵓⵍⴼⵓ ZIP."),
    "تم إنشاء أرشيف ZIP بنجاح، عدد الملفات: " to listOf("ZIP archive created. File count: ", "Archive ZIP créée. Nombre de fichiers : ", "ⵜⵓⵙⵏⵓⵍⴼⴰ ⵜⴰⵔⵛⵉⴼⵜ ZIP. ⵓⵟⵟⵓⵏ ⵏ ⵉⴼⴰⵢⵍⵓⵜⵏ: "),
    "فشل إنشاء ZIP: " to listOf("ZIP creation failed: ", "Échec de création du ZIP : ", "ⵓⵔ ⵉⵏⵊⵉⵃ ⵓⵙⵏⵓⵍⴼⵓ ZIP: "),
    "لا يمكن نسخ مجلد إلى نفسه أو إلى مجلد داخله." to listOf("Cannot copy a folder into itself or one of its subfolders.", "Impossible de copier un dossier dans lui-même ou dans un sous-dossier.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵜⵙⵏⵖⵍⴷ ⴰⴽⴰⵔⵏ ⵖ ⵉⵎⴰⵏⵏⵙ."),
    "اكتملت العملية: " to listOf("Operation complete: ", "Opération terminée : ", "ⵜⴼⴰⴽ ⵜⴰⵎⵙⴰⵙⵜ: "),
    "توقفت العملية بعد " to listOf("Operation stopped after ", "Opération arrêtée après ", "ⵜⵓⵙⵙⵉ ⵜⴰⵎⵙⴰⵙⵜ ⴷⴼⴼⵉⵔ "),
    "تعذر إنشاء المجلد: " to listOf("Could not create folder: ", "Impossible de créer le dossier : ", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵙⵏⵓⵍⴼⵓ ⴰⴽⴰⵔⵏ: "),
    "تعذر إنشاء الملف: " to listOf("Could not create file: ", "Impossible de créer le fichier : ", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵙⵏⵓⵍⴼⵓ ⴰⴼⴰⵢⵍⵓ: "),
    "تعذرت الكتابة إلى الملف." to listOf("Could not write to file.", "Impossible d’écrire dans le fichier.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵔⴰ ⵖ ⵓⴼⴰⵢⵍⵓ."),
    "تعذرت قراءة الملف." to listOf("Could not read file.", "Impossible de lire le fichier.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵖⵔ ⴰⴼⴰⵢⵍⵓ."),
    "تعذر فتح مجلد الحفظ." to listOf("Could not open destination folder.", "Impossible d’ouvrir le dossier de destination.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵍⴷⵉ ⴰⴽⴰⵔⵏ."),
    "تعذر قراءة الأرشيف المحدد." to listOf("Could not read selected archive.", "Impossible de lire l’archive sélectionnée.", "ⵓⵔ ⵉⵣⵎⵉⵔ ⴰⴷ ⵉⵖⵔ ⵜⴰⵔⵛⵉⴼⵜ."),
    "الأرشيف يحتوي على عدد كبير جداً من العناصر." to listOf("Archive contains too many entries.", "L’archive contient trop d’éléments.", "ⵜⴰⵔⵛⵉⴼⵜ ⵜⵙⵄⴰ ⴰⵟⵟⴰⵙ ⵏ ⵉⵙⵖⵓⵏ."),
    "يحتوي الأرشيف على مسار غير آمن." to listOf("Archive contains an unsafe path.", "L’archive contient un chemin non sécurisé.", "ⵜⴰⵔⵛⵉⴼⵜ ⵜⵙⵄⴰ ⴰⴱⵔⵉⴷ ⵓⵔ ⵉⵎⵏ."),
    "تجاوز حجم الاستخراج الحد المسموح (512 ميغابايت)." to listOf("Extraction exceeded the 512 MB limit.", "L’extraction a dépassé la limite de 512 Mo.", "ⵉⵄⴷⴰ ⵓⵙⵎⵓⵔⵔⵉ ⵍⵃⴷ ⵏ 512 MB."),
    "تم استخراج " to listOf("Extracted ", "Extrait ", "ⵉⵜⵜⵓⵙⵎⵓⵔⵔⵉ "),
    " ملف بنجاح." to listOf(" file(s) successfully.", " fichier(s) avec succès.", " ⵉⴼⴰⵢⵍⵓⵜⵏ ⵙ ⵓⵏⴼⵍⵓⵍ."),
    "فشل فك الضغط: " to listOf("Extraction failed: ", "Échec de l’extraction : ", "ⵓⵔ ⵉⵏⵊⵉⵃ ⵓⵙⵎⵓⵔⵔⵉ: ")
)

private fun fileMasterText(text: String, language: String): String {
    if (language == "ar") return text
    val index = when (language) { "en" -> 0; "fr" -> 1; "tz" -> 2; else -> return text }
    return (FileMasterTranslations[text] ?: FileMasterExtraTranslations[text])?.getOrNull(index) ?: text
}

private fun fileMasterLanguageName(language: String): String = when (language) {
    "ar" -> "العربية"; "en" -> "English"; "fr" -> "Français"; "tz" -> "ⵜⴰⵎⴰⵣⵉⵖⵜ"; else -> "العربية"
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FileMasterApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("filemaster_preferences", android.content.Context.MODE_PRIVATE) }
    var language by remember { mutableStateOf(preferences.getString("language", "ar") ?: "ar") }
    var darkMode by remember { mutableStateOf(preferences.getBoolean("dark_theme", true)) }
    LaunchedEffect(darkMode) { setFileMasterPalette(darkMode) }
    var favoriteUris by remember { mutableStateOf(preferences.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var recentUris by remember { mutableStateOf(preferences.getString("recent", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var imagesOnly by remember { mutableStateOf(false) }
    var recentOnly by remember { mutableStateOf(false) }
    var largeOnly by remember { mutableStateOf(false) }
    var languageMenuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var privacyOpen by remember { mutableStateOf(false) }
    var deviceInfoOpen by remember { mutableStateOf(false) }
    var deviceMetrics by remember { mutableStateOf("اضغط على زر القياس لقراءة CPU وسرعة التنزيل.") }
    var measuringMetrics by remember { mutableStateOf(false) }
    var qrResult by remember { mutableStateOf<String?>(null) }
    fun tr(text: String): String = fileMasterText(text, language)
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedZip by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var destination by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(tr("اختر أرشيف ZIP للبدء.")) }
    var success by remember { mutableStateOf(false) }
    var browseFolder by remember { mutableStateOf<DocumentFile?>(null) }
    var browseRootUri by remember { mutableStateOf<Uri?>(null) }
    var browseFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var zipSelection by remember { mutableStateOf(setOf<String>()) }
    var browsePage by remember { mutableIntStateOf(0) }
    var browseSearch by remember { mutableStateOf("") }
    var zipSources by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var managerDialog by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf("") }
    var moveOperation by remember { mutableStateOf(false) }
    var trashMode by remember { mutableStateOf(false) }
    var trashReturnFolder by remember { mutableStateOf<DocumentFile?>(null) }
    var hiddenMode by remember { mutableStateOf(false) }
    var hiddenReturnFolder by remember { mutableStateOf<DocumentFile?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var previewTitle by remember { mutableStateOf("") }
    var previewLoading by remember { mutableStateOf(false) }
    var archivePreviewText by remember { mutableStateOf("") }
    var privacyPinHash by remember { mutableStateOf(preferences.getString("privacy_pin_hash", null)) }
    var isLocked by remember { mutableStateOf(privacyPinHash != null) }
    var pinEntry by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    var lanServer by remember { mutableStateOf<LocalFileShareServer?>(null) }
    var lanShareUrl by remember { mutableStateOf("") }
    DisposableEffect(lanServer) { onDispose { lanServer?.close() } }

    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedZip = uri
            selectedName = DocumentFile.fromSingleUri(context, uri)?.name ?: "archive.zip"
            success = false
            status = tr("تم اختيار الأرشيف. اختر مجلد الحفظ لفك الضغط.")
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
            status = tr("تم اختيار مجلد الحفظ. اضغط «فك الضغط الآن».")
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
            trashMode = false
            trashReturnFolder = null
            hiddenMode = false
            hiddenReturnFolder = null
            browsePage = 0
            browseFiles = browseFolder?.listFiles()?.filter { it.name != ".FileMasterTrash" && it.name != ".FileMasterHidden" }?.sortedWith(
                compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }
            ) ?: emptyList()
            zipSelection = emptySet()
            status = tr("تم فتح مجلد الذاكرة. اضغط على مجلد للدخول أو حدد ملفات لإنشاء ZIP.")
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
                status = if (moveOperation) tr("جارٍ نقل العناصر…") else tr("جارٍ نسخ العناصر…")
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
            status = tr("جارٍ إنشاء أرشيف ZIP…")
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

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(
        primary = Accent, onPrimary = Color(0xFF06211E), background = Ink, surface = Panel,
        onSurface = Color(0xFFF1F5F9), secondary = Muted
    ) else lightColorScheme(
        primary = Accent, onPrimary = Color.White, background = Ink, surface = Panel,
        onSurface = Color(0xFF172033), secondary = Muted
    )) {
        if (isLocked) {
            Column(Modifier.fillMaxSize().background(Ink).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = Accent, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("FileMaster", color = Color(0xFFF4F7FB), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("أدخل رمز الخصوصية لفتح ملفاتك", color = Muted)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = pinEntry, onValueChange = { pinEntry = it.filter(Char::isDigit).take(12); pinError = "" }, label = { Text("رمز PIN") }, singleLine = true)
                if (pinError.isNotBlank()) Text(pinError, color = Color(0xFFFF8A80))
                Button(onClick = {
                    if (hashPrivacyPin(pinEntry) == privacyPinHash) { isLocked = false; pinEntry = ""; pinError = "" }
                    else pinError = "الرمز غير صحيح."
                }, modifier = Modifier.fillMaxWidth()) { Text("فتح التطبيق") }
                TextButton(onClick = {
                    if (hashPrivacyPin(pinEntry) == privacyPinHash) {
                        preferences.edit().remove("privacy_pin_hash").apply()
                        privacyPinHash = null; isLocked = false; pinEntry = ""; pinError = ""
                    } else pinError = "أدخل الرمز الصحيح لإزالة القفل."
                }) { Text("إزالة القفل باستخدام الرمز") }
            }
        } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = Panel) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            Modifier.fillMaxWidth().background(
                                Brush.linearGradient(listOf(Accent.copy(alpha = 0.24f), Panel)),
                                RoundedCornerShape(22.dp)
                            ).padding(18.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = Accent, modifier = Modifier.size(38.dp))
                                Text("FileMaster", color = Color(0xFFF4F7FB), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("ملفاتك. جهازك. تحكمك.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                Text("By Ahmedbecetti", color = Accent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        NavigationDrawerItem(icon = { Icon(Icons.Default.FolderOpen, null) }, label = { Text("مستكشف الملفات") }, selected = false, onClick = { scope.launch { drawerState.close() } })
                        NavigationDrawerItem(icon = { Icon(Icons.Default.Memory, null) }, label = { Text("صحة الهاتف والفحص") }, selected = false, onClick = { scope.launch { drawerState.close() }; deviceInfoOpen = true })
                        NavigationDrawerItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("الإعدادات واللغة") }, selected = false, onClick = { scope.launch { drawerState.close() }; settingsOpen = true })
                        NavigationDrawerItem(icon = { Icon(Icons.Default.Info, null) }, label = { Text("الخصوصية") }, selected = false, onClick = { scope.launch { drawerState.close() }; privacyOpen = true })
                        Spacer(Modifier.height(10.dp))
                        Text("إدارة ملفات وأرشيفات • 1.2", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        ) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("FileMaster", fontWeight = FontWeight.Bold)
                            Text(tr("مدير الملفات والأرشيفات"), style = MaterialTheme.typography.labelMedium, color = Muted)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "فتح القائمة", tint = Accent)
                        }
                    },
                    actions = {
                        if (privacyPinHash != null) {
                            IconButton(onClick = { pinEntry = ""; pinError = ""; isLocked = true }) {
                                Icon(Icons.Default.Storage, contentDescription = tr("قفل الخصوصية"), tint = Accent)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AdMobBanner(modifier = Modifier.fillMaxWidth())
                DeviceHealthDashboard(context = context, folder = browseFolder, darkMode = darkMode)
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
                        Text(tr("ملفاتك، تحت سيطرتك"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            tr("تصفح ملفات الهاتف واختر الأرشيف ومجلد الاستخراج. تتم عمليات الملفات على جهازك."),
                            style = MaterialTheme.typography.bodyMedium, color = Muted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = Accent)
                            Text(tr("وصول آمن إلى التخزين عبر Android"), style = MaterialTheme.typography.labelMedium, color = Accent)
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
                                Text(tr("مستكشف ذاكرة الهاتف"), fontWeight = FontWeight.Bold)
                                Text(browseFolder?.name ?: tr("اختر مجلداً لعرض محتوياته"), color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { browsePicker.launch(null) }) { Text(tr("تصفح")) }
                        }
                        if (browseFolder != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    if (trashMode || hiddenMode) {
                                        val back = if (trashMode) trashReturnFolder else hiddenReturnFolder
                                        if (back != null) {
                                            browseFolder = back
                                            browseFiles = back.listFiles().filter { it.name != ".FileMasterTrash" && it.name != ".FileMasterHidden" }.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                        }
                                        trashMode = false
                                        trashReturnFolder = null
                                        hiddenMode = false
                                        hiddenReturnFolder = null
                                    } else {
                                        val parent = browseFolder?.parentFile
                                        if (parent != null) {
                                            browsePage = 0
                                            browseFolder = parent
                                            browseFiles = parent.listFiles().filter { it.name != ".FileMasterTrash" && it.name != ".FileMasterHidden" }.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                            zipSelection = emptySet()
                                        }
                                    }
                                }) { Text(if (trashMode) tr("العودة من سلة المحذوفات") else if (hiddenMode) tr("العودة من الملفات المخفية") else tr("المجلد السابق")) }
                                OutlinedButton(onClick = {
                                    destination = browseRootUri
                                    status = tr("تم تعيين المجلد الرئيسي المختار كوجهة لفك الضغط.")
                                    success = true
                                }) { Text(tr("استخدام كوجهة")) }
                                if (!trashMode && !hiddenMode) OutlinedButton(onClick = {
                                    val current = browseFolder
                                    val trash = current?.let { getOrCreateTrashFolder(it) }
                                    if (trash == null) {
                                        status = tr("تعذر إنشاء سلة المحذوفات في هذا المجلد.")
                                        success = false
                                    } else {
                                        trashReturnFolder = current
                                        browseFolder = trash
                                        browseFiles = trash.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                        trashMode = true
                                        zipSelection = emptySet()
                                        browsePage = 0
                                        status = tr("سلة المحذوفات: حدد العناصر لاستعادتها.")
                                    }
                                }) { Text(tr("سلة المحذوفات")) }
                                if (!trashMode && !hiddenMode) OutlinedButton(onClick = {
                                    val current = browseFolder
                                    val hidden = current?.let { getOrCreateHiddenFolder(it) }
                                    if (hidden == null) { status = tr("تعذر إنشاء مجلد الملفات المخفية."); success = false }
                                    else {
                                        hiddenReturnFolder = current
                                        browseFolder = hidden
                                        browseFiles = hidden.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                        hiddenMode = true
                                        zipSelection = emptySet()
                                        browsePage = 0
                                        status = tr("الملفات المخفية: حدد العناصر ثم أظهرها لاستعادتها.")
                                    }
                                }) { Text(tr("الملفات المخفية")) }
                            }
                            if ((trashMode || hiddenMode) && zipSelection.isNotEmpty()) {
                                OutlinedButton(onClick = {
                                    val target = if (trashMode) trashReturnFolder else hiddenReturnFolder
                                    val selected = browseFiles.filter { it.uri.toString() in zipSelection }
                                    if (target == null) {
                                        status = tr("اختر مجلد الملفات الأصلي أولاً.")
                                        success = false
                                    } else {
                                        val result = restoreFromTrash(context, selected, target)
                                        status = result
                                        success = result.startsWith("تمت استعادة")
                                        browseFiles = browseFolder?.listFiles()?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                                        zipSelection = emptySet()
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text(if (trashMode) tr("استعادة العناصر المحددة") else tr("إظهار العناصر المحددة")) }
                            }
                            OutlinedTextField(
                                value = browseSearch,
                                onValueChange = { browseSearch = it; browsePage = 0 },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(tr("ابحث في هذا المجلد")) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            val matchingBrowseFiles = browseFiles.filter { file ->
                                val name = file.name.orEmpty()
                                name.contains(browseSearch, ignoreCase = true) &&
                                    (!favoritesOnly || file.uri.toString() in favoriteUris) &&
                                    (!imagesOnly || name.substringAfterLast(".", "").lowercase() in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")) &&
                                    (!recentOnly || file.uri.toString() in recentUris) &&
                                    (!largeOnly || (!file.isDirectory && file.length() >= 10L * 1024L * 1024L))
                            }
                            val folderBytes = browseFiles.sumOf { it.length().coerceAtLeast(0L) }
                            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (trashMode) tr("سلة المحذوفات") else if (hiddenMode) tr("الملفات المخفية") else tr("محتويات المجلد"), fontWeight = FontWeight.Bold, color = Accent)
                                    Text("${browseFiles.size} items • ${formatFileSize(folderBytes)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly; imagesOnly = false; browsePage = 0 }, label = { Text("Favorites") })
                                        FilterChip(selected = imagesOnly, onClick = { imagesOnly = !imagesOnly; favoritesOnly = false; recentOnly = false; largeOnly = false; browsePage = 0 }, label = { Text("Images") })
                                        FilterChip(selected = recentOnly, onClick = { recentOnly = !recentOnly; favoritesOnly = false; imagesOnly = false; largeOnly = false; browsePage = 0 }, label = { Text("Recent") })
                                        FilterChip(selected = largeOnly, onClick = { largeOnly = !largeOnly; favoritesOnly = false; imagesOnly = false; recentOnly = false; browsePage = 0 }, label = { Text("10 MB+") })
                                        TextButton(onClick = { favoritesOnly = false; imagesOnly = false; recentOnly = false; largeOnly = false; browseSearch = ""; browsePage = 0 }) { Text("All") }
                                    }
                                    if (recentUris.isNotEmpty()) Text("Recently opened: ${recentUris.size}", color = Muted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (matchingBrowseFiles.isEmpty()) {
                                Text(if (browseFiles.isEmpty()) tr("المجلد فارغ") else tr("لا توجد نتائج مطابقة"), color = Muted, style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = { managerName = ""; managerDialog = "mkdir" }, modifier = Modifier.fillMaxWidth()) { Text(tr("إنشاء مجلد هنا")) }
                            } else {
                                matchingBrowseFiles.drop(browsePage * 8).take(8).forEach { file ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            if (file.isDirectory) {
                                                browsePage = 0
                                                browseFolder = file
                                                browseFiles = file.listFiles().filter { it.name != ".FileMasterTrash" && it.name != ".FileMasterHidden" }.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                                zipSelection = emptySet()
                                            } else {
                                                val fileName = file.name ?: "file"
                                                recentUris = (listOf(file.uri.toString()) + recentUris.filterNot { it == file.uri.toString() }).take(20)
                                                preferences.edit().putString("recent", recentUris.joinToString("\n")).apply()
                                                val extension = fileName.substringAfterLast('.', "").lowercase()
                                                if (extension in setOf("zip", "7z", "rar", "tar", "tgz", "gz")) {
                                                    selectedZip = file.uri
                                                    selectedName = fileName
                                                    status = tr("تم اختيار ") + fileName + tr(". استخدم زر فك الضغط لاستخراجه.")
                                                    success = true
                                                } else if (extension in setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")) {
                                                    previewTitle = fileName
                                                    previewBitmap = null
                                                    previewLoading = true
                                                    scope.launch {
                                                        val bitmap = withContext(Dispatchers.IO) {
                                                            try { context.contentResolver.openInputStream(file.uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } }
                                                            catch (_: Exception) { null }
                                                        }
                                                        previewBitmap = bitmap
                                                        previewLoading = false
                                                        status = if (bitmap != null) tr("تم فتح المعاينة: ") + fileName else tr("تعذرت معاينة الصورة.")
                                                        success = bitmap != null
                                                    }
                                                } else {
                                                    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(file.uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                                        status = tr("تم فتح ") + fileName + tr(" باستخدام التطبيق المناسب.")
                                                        success = true
                                                    } catch (_: ActivityNotFoundException) {
                                                        status = tr("لا يوجد تطبيق مثبت لفتح هذا النوع من الملفات.")
                                                        success = false
                                                    } catch (e: Exception) {
                                                        status = tr("تعذر فتح الملف: ") + (e.localizedMessage ?: tr("خطأ غير معروف"))
                                                        success = false
                                                    }
                                                }
                                            }
                                        }.padding(vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (file.isDirectory) Icons.Default.FolderOpen else Icons.Default.InsertDriveFile, contentDescription = null, tint = Accent)
                                        Text(file.name ?: tr("بدون اسم"), modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, color = Color(0xFFE0E6EF))
                                        IconButton(onClick = {
                                            val key = file.uri.toString()
                                            favoriteUris = if (key in favoriteUris) favoriteUris - key else favoriteUris + key
                                            preferences.edit().putStringSet("favorites", favoriteUris).apply()
                                        }) { Icon(Icons.Default.Star, contentDescription = "Favorite", tint = if (file.uri.toString() in favoriteUris) Accent else Muted) }
                                        Checkbox(
                                            checked = file.uri.toString() in zipSelection,
                                            onCheckedChange = { checked ->
                                                zipSelection = if (checked) zipSelection + file.uri.toString() else zipSelection - file.uri.toString()
                                            }
                                        )
                                    }
                                }
                                if (matchingBrowseFiles.size > 8) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton(enabled = browsePage > 0, onClick = { browsePage-- }) { Text(tr("السابق")) }
                                        Text((browsePage + 1).toString() + " / " + ((matchingBrowseFiles.size + 7) / 8).toString(), color = Muted)
                                        TextButton(enabled = (browsePage + 1) * 8 < matchingBrowseFiles.size, onClick = { browsePage++ }) { Text(tr("التالي")) }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { managerName = ""; managerDialog = "mkdir" }, modifier = Modifier.weight(1f)) { Text(tr("مجلد جديد")) }
                                    OutlinedButton(onClick = {
                                        if (zipSelection.size == 1) {
                                            managerName = browseFiles.firstOrNull { it.uri.toString() in zipSelection }?.name.orEmpty()
                                            managerDialog = "rename"
                                        } else status = tr("حدد عنصراً واحداً لإعادة تسميته.")
                                    }, modifier = Modifier.weight(1f)) { Text(tr("إعادة تسمية")) }
                                    OutlinedButton(onClick = {
                                        val selected = browseFiles.filter { it.uri.toString() in zipSelection }
                                        if (selected.isEmpty()) status = tr("حدد عناصر لإخفائها أولاً.")
                                        else {
                                            val hidden = browseFolder?.let { getOrCreateHiddenFolder(it) }
                                            if (hidden == null) { status = tr("تعذر إنشاء مجلد الملفات المخفية."); success = false }
                                            else {
                                                val result = moveToTrash(context, selected, hidden, "المجلد المخفي")
                                                status = result
                                                success = result.startsWith("نُقلت العناصر إلى المجلد المخفي")
                                                browseFiles = browseFolder?.listFiles()?.filter { it.name != ".FileMasterTrash" && it.name != ".FileMasterHidden" }?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                                                zipSelection = emptySet()
                                            }
                                        }
                                    }, modifier = Modifier.weight(1f)) { Text(tr("إخفاء المحدد")) }
                                    OutlinedButton(onClick = { managerDialog = "delete" }, modifier = Modifier.weight(1f)) { Text(tr("حذف")) }
                                }
                                OutlinedButton(
                                    onClick = {
                                        busy = true
                                        status = "Scanning selected folder for duplicate files…"
                                        val candidates = browseFiles.filter { !it.isDirectory }
                                        scope.launch {
                                            val duplicates = withContext(Dispatchers.IO) { findDuplicateDocuments(context, candidates) }
                                            busy = false
                                            status = if (duplicates.isEmpty()) "No duplicate files found in this folder." else "Possible duplicates found: " + duplicates.joinToString(", ") { it.name ?: "file" }
                                            success = duplicates.isEmpty()
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Scan current folder for duplicates") }
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { moveOperation = false; operationDestinationPicker.launch(null) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, null); Text(tr("نسخ")) }
                                    OutlinedButton(onClick = { moveOperation = true; operationDestinationPicker.launch(null) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DriveFileMove, null); Text(tr("نقل")) }
                                }
                                OutlinedButton(
                                    onClick = {
                                        zipSources = browseFiles.filter { it.uri.toString() in zipSelection }
                                        if (zipSources.isEmpty()) status = tr("حدد ملفاً واحداً على الأقل لإنشاء ZIP.")
                                        else zipCreator.launch("FileMaster-archive.zip")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(tr("إنشاء ZIP من العناصر المحددة (") + zipSelection.size + ")") }
                            }
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("حالة الجهاز", fontWeight = FontWeight.Bold, color = Accent)
                        val am = remember { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
                        val mi = remember { ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) } }
                        Text("RAM المتاحة: " + formatFileSize(mi.availMem) + " من " + formatFileSize(mi.totalMem), color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text("ملاحظة: لا يوفّر Android قياساً موحداً موثوقاً لسرعة الإنترنت أو نسبة CPU لجميع التطبيقات.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = {
                            val selected = browseFiles.filter { it.uri.toString() in zipSelection && !it.isDirectory }
                            if (selected.isEmpty()) { status = "حدد ملفات أولاً للإرسال."; success = false } else {
                                val sendIntent = Intent(if (selected.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "*/*"
                                    if (selected.size == 1) putExtra(Intent.EXTRA_STREAM, selected.first().uri)
                                    else putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selected.map { it.uri }))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try { context.startActivity(Intent.createChooser(sendIntent, "إرسال عبر Bluetooth أو تطبيق Wi‑Fi"))
                                    status = "اختر Bluetooth أو تطبيق مشاركة متوافق من قائمة Android."; success = true
                                } catch (e: Exception) { status = "تعذر فتح قائمة المشاركة: " + (e.localizedMessage ?: "خطأ"); success = false }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Share, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("إرسال الملفات عبر تطبيقات المشاركة") }
                        if (lanShareUrl.isNotBlank()) {
                            Text("رابط LAN: " + lanShareUrl, color = Accent, style = MaterialTheme.typography.bodySmall)
                            Text("أي جهاز على الشبكة المحلية يملك الرابط يمكنه تنزيل الملفات المحددة. أوقف المشاركة بعد الانتهاء.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = {
                            if (lanServer != null) {
                                lanServer?.close()
                                lanServer = null
                                lanShareUrl = ""
                                status = "تم إيقاف مشاركة الشبكة المحلية."
                                success = true
                            } else {
                                val selected = browseFiles.filter { it.uri.toString() in zipSelection && !it.isDirectory }
                                if (selected.isEmpty()) {
                                    status = "حدد ملفاً واحداً على الأقل أولاً."
                                    success = false
                                } else {
                                    try {
                                        val server = LocalFileShareServer(context, selected)
                                        lanServer = server
                                        lanShareUrl = server.url
                                        scope.launch(Dispatchers.IO) { server.serve() }
                                        status = "بدأت مشاركة الملفات على الشبكة المحلية: " + server.url
                                        success = true
                                    } catch (e: Exception) {
                                        status = "تعذر بدء مشاركة الشبكة المحلية: " + (e.localizedMessage ?: "خطأ")
                                        success = false
                                    }
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (lanServer == null) "مشاركة الملفات عبر الشبكة المحلية" else "إيقاف مشاركة الشبكة المحلية")
                        }
                        OutlinedButton(onClick = {
                            val junkExtensions = setOf("tmp", "temp", "log", "bak", "old", "part", "crdownload", "cache")
                            val candidates = browseFiles.filter { file ->
                                if (file.isDirectory || file.name == ".FileMasterTrash" || file.name == ".FileMasterHidden") false
                                else {
                                    val name = file.name.orEmpty().lowercase()
                                    val ext = name.substringAfterLast('.', "")
                                    ext in junkExtensions || name == "thumbs.db" || name == ".ds_store" || name.startsWith("~$")
                                }
                            }
                            zipSelection = candidates.map { it.uri.toString() }.toSet()
                            status = if (candidates.isEmpty()) "لم يُعثر على ملفات مؤقتة واضحة في المجلد الحالي."
                                else "عُثر على " + candidates.size + " ملفاً مرشحاً للتنظيف. راجع العناصر المحددة؛ زر الحذف ينقلها إلى سلة المحذوفات."
                            success = candidates.isEmpty()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("فحص الملفات المؤقتة بأمان")
                        }
                    }
                }
                
                ActionCard(
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Accent) },
                    title = "ماسح QR",
                    detail = qrResult ?: "امسح رمز QR أو باركود بالكاميرا",
                    buttonText = "مسح الآن",
                    onClick = {
                        GmsBarcodeScanning.getClient(context).startScan()
                            .addOnSuccessListener { barcode ->
                                qrResult = barcode.rawValue ?: "تمت قراءة الرمز دون نص"
                                status = qrResult ?: "تمت القراءة"
                                success = true
                            }
                            .addOnCanceledListener { status = "تم إلغاء المسح"; success = false }
                            .addOnFailureListener { error ->
                                status = "تعذر تشغيل ماسح QR: " + (error.localizedMessage ?: "خطأ غير معروف")
                                success = false
                            }
                    }
                )

                Text(tr("أدوات سريعة"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ActionCard(
                    icon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Accent) },
                    title = tr("اختيار أرشيف"),
                    detail = selectedName ?: tr("اختر ملفاً من ذاكرة الهاتف"),
                    buttonText = tr("تصفح الملفات"),
                    onClick = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/x-7z-compressed", "application/vnd.rar", "application/x-rar-compressed", "application/x-tar", "application/gzip", "application/x-gzip", "application/octet-stream", "*/*")) }
                )
                OutlinedButton(onClick = {
                    val archive = selectedZip
                    val archiveName = selectedName
                    if (archive == null || archiveName == null) {
                        status = tr("اختر أرشيفاً أولاً.")
                        success = false
                    } else {
                        busy = true
                        scope.launch {
                            archivePreviewText = withContext(Dispatchers.IO) { previewArchiveContents(context, archive, archiveName) }
                            busy = false
                        }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(tr("معاينة محتويات الأرشيف")) }
                ActionCard(
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Accent) },
                    title = tr("مجلد الاستخراج"),
                    detail = if (destination == null) tr("اختر المكان الذي تريد حفظ الملفات فيه") else tr("تم تحديد مجلد الحفظ"),
                    buttonText = tr("اختيار المجلد"),
                    onClick = { folderPicker.launch(null) }
                )

                Button(
                    onClick = {
                        val archive = selectedZip
                        val target = destination
                        if (archive == null) {
                            status = tr("اختر أرشيفاً أولاً.")
                            success = false
                        } else if (target == null) {
                            status = tr("اختر مجلد الحفظ أولاً.")
                            success = false
                        } else {
                            busy = true
                            success = false
                            status = tr("جارٍ فك الضغط…")
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { extractArchiveSafely(context, archive, selectedName ?: "archive.zip", target) }
                                busy = false
                                success = result.startsWith(tr("تم استخراج"))
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
                    Text(if (busy) tr("جارٍ الاستخراج…") else tr("فك الضغط الآن"), fontWeight = FontWeight.Bold)
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
                    tr("النسخة 1.0 • صُمّم بواسطة Ahmedbecett"),
                    modifier = Modifier.fillMaxWidth(), color = Muted, style = MaterialTheme.typography.labelSmall
                )
            }
        }
        }
        if (archivePreviewText.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { archivePreviewText = "" },
                title = { Text(tr("معاينة محتويات الأرشيف")) },
                text = { Text(archivePreviewText, modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { archivePreviewText = "" }) { Text(tr("إغلاق المعاينة")) } }
            )
        }
        if (previewLoading || previewBitmap != null) {
            Dialog(onDismissRequest = { previewBitmap = null; previewTitle = ""; previewLoading = false }) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(previewTitle, color = Color(0xFFF4F7FB), fontWeight = FontWeight.Bold, maxLines = 2)
                        when {
                            previewLoading -> CircularProgressIndicator(color = Accent)
                            previewBitmap != null -> Image(bitmap = previewBitmap!!.asImageBitmap(), contentDescription = previewTitle, modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp), contentScale = ContentScale.Fit)
                            else -> Text(tr("تعذرت معاينة الصورة."), color = Muted)
                        }
                        TextButton(onClick = { previewBitmap = null; previewTitle = ""; previewLoading = false }) { Text(tr("إغلاق المعاينة")) }
                    }
                }
            }
        }
        if (managerDialog == "pinSetup") {
            AlertDialog(
                onDismissRequest = { managerDialog = ""; managerName = ""; pinError = "" },
                title = { Text("إعداد قفل الخصوصية") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أنشئ رمز PIN من 4 أرقام على الأقل. هذا القفل يحجب واجهة التطبيق ولا يشفر الملفات نفسها.", color = Muted)
                        OutlinedTextField(value = managerName, onValueChange = { managerName = it.filter(Char::isDigit).take(12); pinError = "" }, label = { Text("رمز PIN جديد") }, singleLine = true)
                        if (pinError.isNotBlank()) Text(pinError, color = Color(0xFFFF8A80))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (managerName.length < 4) pinError = "يجب أن يتكون الرمز من 4 أرقام على الأقل."
                        else {
                            val hash = hashPrivacyPin(managerName)
                            preferences.edit().putString("privacy_pin_hash", hash).apply()
                            privacyPinHash = hash
                            managerName = ""; managerDialog = ""; pinError = ""
                            status = "تم إعداد قفل الخصوصية."; success = true
                        }
                    }) { Text("حفظ الرمز") }
                },
                dismissButton = { TextButton(onClick = { managerDialog = ""; managerName = ""; pinError = "" }) { Text("إلغاء") } }
            )
        }
        if (managerDialog == "mkdir" || managerDialog == "rename") {
            AlertDialog(
                onDismissRequest = { managerDialog = "" },
                title = { Text(if (managerDialog == "mkdir") tr("إنشاء مجلد") else tr("إعادة تسمية")) },
                text = { OutlinedTextField(value = managerName, onValueChange = { managerName = it }, label = { Text(tr("الاسم")) }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        val safeName = managerName.trim()
                        if (safeName.isBlank() || safeName == "." || safeName == ".." || safeName.contains("/") || safeName.contains(":")) {
                            status = tr("أدخل اسماً صالحاً.")
                        } else {
                            val selectedFile = browseFiles.firstOrNull { it.uri.toString() in zipSelection }
                            val ok = if (managerDialog == "mkdir") browseFolder?.findFile(safeName) == null && browseFolder?.createDirectory(safeName) != null else selectedFile?.renameTo(safeName) == true
                            status = if (ok) tr("تمت العملية بنجاح.") else tr("تعذرت العملية؛ تحقق من الصلاحيات والاسم.")
                            managerDialog = ""
                            browseFiles = browseFolder?.listFiles()?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                            zipSelection = emptySet()
                        }
                    }) { Text(tr("تنفيذ")) }
                },
                dismissButton = { TextButton(onClick = { managerDialog = "" }) { Text(tr("إلغاء")) } }
            )
        }
        if (settingsOpen) {
            AlertDialog(
                onDismissRequest = { settingsOpen = false },
                title = { Text("إعدادات FileMaster") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("المطور: Ahmedbecetti")
                        Text("الدعم: ahmedbecetti41@gmail.com")
                        Text("اللغة: " + fileMasterLanguageName(language))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ar", "en", "fr", "tz").forEach { lang ->
                                OutlinedButton(onClick = {
                                    language = lang
                                    preferences.edit().putString("language", lang).apply()
                                }) { Text(fileMasterLanguageName(lang)) }
                            }
                        }
                        OutlinedButton(onClick = { settingsOpen = false; managerName = ""; pinError = ""; managerDialog = "pinSetup" }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (privacyPinHash == null) "إعداد قفل الخصوصية" else "تغيير/إعادة إعداد رمز الخصوصية")
                        }
                        OutlinedButton(onClick = {
                            darkMode = !darkMode
                            preferences.edit().putBoolean("dark_theme", darkMode).apply()
                            setFileMasterPalette(darkMode)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (darkMode) "التبديل إلى المظهر الفاتح" else "التبديل إلى المظهر الداكن")
                        }
                        OutlinedButton(onClick = { settingsOpen = false; privacyOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("ملخص الخصوصية")
                        }
                        OutlinedButton(
                            onClick = {
                                val policyIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Ahmedbecett/FileMaster-Android/blob/main/PRIVACY.md"))
                                runCatching { context.startActivity(policyIntent) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("فتح سياسة الخصوصية المنشورة") }
                        OutlinedButton(onClick = { settingsOpen = false; deviceInfoOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("معلومات الجهاز والذاكرة")
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { settingsOpen = false }) { Text("إغلاق") } }
            )
        }
        if (privacyOpen) {
            AlertDialog(
                onDismissRequest = { privacyOpen = false },
                title = { Text("سياسة الخصوصية — ملخص") },
                text = { Text("تعمل عمليات إدارة الملفات محلياً على جهازك. قد تستخدم النسخة الحالية إعلانات Google Mobile Ads، والتي قد تعالج بيانات وفق إعدادات وسياسات Google. لا ترفع ملفاتك إلى خوادم FileMaster ضمن وظائف إدارة الملفات الحالية. هذه شاشة ملخص وليست بديلاً عن نشر سياسة قانونية كاملة ورابطها قبل الإصدار العام.") },
                confirmButton = { TextButton(onClick = { privacyOpen = false }) { Text("فهمت") } }
            )
        }
        if (deviceInfoOpen) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            AlertDialog(
                onDismissRequest = { if (!measuringMetrics) deviceInfoOpen = false },
                title = { Text("معلومات الجهاز") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("الذاكرة الكلية: " + formatFileSize(mi.totalMem))
                        Text("الذاكرة المتاحة حالياً: " + formatFileSize(mi.availMem))
                        Text("الذاكرة المستخدمة تقريبياً: " + formatFileSize((mi.totalMem - mi.availMem).coerceAtLeast(0)))
                        HorizontalDivider()
                        Text(deviceMetrics, color = Muted, style = MaterialTheme.typography.bodyMedium)
                        Text("اختبار الإنترنت ينزّل حتى 1 ميغابايت عند الضغط على الزر.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        if (measuringMetrics) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Button(
                            enabled = !measuringMetrics,
                            onClick = {
                                measuringMetrics = true
                                deviceMetrics = "جارٍ قياس استخدام المعالج وسرعة التنزيل…"
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        val cpu = measureCpuUsagePercent()
                                        val speed = measureInternetDownloadMbps()
                                        val cpuText = cpu?.let { String.format(java.util.Locale.US, "%.1f%%", it) } ?: "غير متاح"
                                        val speedText = speed?.let { String.format(java.util.Locale.US, "%.2f Mbps", it) } ?: "تعذر القياس؛ تحقق من اتصال الإنترنت"
                                        "استخدام CPU التقريبي: $cpuText\nسرعة التنزيل التقريبية: $speedText"
                                    }
                                    deviceMetrics = result
                                    measuringMetrics = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (measuringMetrics) "جارٍ القياس…" else "قياس CPU وسرعة الإنترنت") }
                    }
                },
                confirmButton = { TextButton(enabled = !measuringMetrics, onClick = { deviceInfoOpen = false }) { Text("إغلاق") } }
            )
        }
        if (managerDialog == "delete") {
            AlertDialog(
                onDismissRequest = { managerDialog = "" },
                title = { Text(tr("حذف العناصر المحددة؟")) },
                text = { Text(if (trashMode) tr("سيتم حذف العناصر المحددة نهائياً.") else tr("ستنقل العناصر المحددة إلى سلة المحذوفات ويمكن استعادتها لاحقاً.")) },
                confirmButton = {
                    TextButton(onClick = {
                        val targets = browseFiles.filter { it.uri.toString() in zipSelection }
                        val result = if (trashMode) {
                            val removed = targets.count { it.delete() }
                            tr("حُذف نهائياً ") + removed + tr(" من ") + targets.size + tr(" عنصر.")
                        } else {
                            val trashParent = if (hiddenMode) hiddenReturnFolder else browseFolder
                            val trash = trashParent?.let { getOrCreateTrashFolder(it) }
                            if (trash == null) tr("تعذر إنشاء سلة المحذوفات؛ لم تُحذف الملفات.") else moveToTrash(context, targets, trash)
                        }
                        status = result
                        success = result.startsWith("نُقلت العناصر إلى سلة المحذوفات") || result.startsWith("حُذف نهائياً")
                        managerDialog = ""
                        zipSelection = emptySet()
                        browseFiles = browseFolder?.listFiles()?.filter { (trashMode || hiddenMode) || (it.name != ".FileMasterTrash" && it.name != ".FileMasterHidden") }?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                    }) { Text(tr("حذف")) }
                },
                dismissButton = { TextButton(onClick = { managerDialog = "" }) { Text(tr("إلغاء")) } }
            )
        }
        }
    }
}


@Composable
private fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-6858364141205978/8292299771"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
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

private fun hashPrivacyPin(pin: String): String {
    val input = "FileMaster::PrivacyLock::v1:" + pin
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

private fun getOrCreateHiddenFolder(parent: DocumentFile): DocumentFile? {
    val existing = parent.findFile(".FileMasterHidden")
    if (existing != null) return existing.takeIf { it.isDirectory }
    return parent.createDirectory(".FileMasterHidden")
}

private fun getOrCreateTrashFolder(parent: DocumentFile): DocumentFile? {
    val existing = parent.findFile(".FileMasterTrash")
    if (existing != null) return existing.takeIf { it.isDirectory }
    return parent.createDirectory(".FileMasterTrash")
}

private fun uniqueDocumentName(parent: DocumentFile, original: String): String {
    if (parent.findFile(original) == null) return original
    val dot = original.lastIndexOf('.')
    val stem = if (dot > 0) original.substring(0, dot) else original
    val ext = if (dot > 0) original.substring(dot) else ""
    var index = 1
    while (index < 10000) {
        val candidate = stem + "-restored-" + index + ext
        if (parent.findFile(candidate) == null) return candidate
        index++
    }
    return System.currentTimeMillis().toString() + "-" + original
}

private fun moveToTrash(context: android.content.Context, sources: List<DocumentFile>, trash: DocumentFile, label: String = "سلة المحذوفات"): String {
    var moved = 0
    val errors = mutableListOf<String>()
    for (source in sources) {
        var staging: DocumentFile? = null
        try {
            val name = uniqueDocumentName(trash, source.name ?: "item")
            val target = (if (source.isDirectory) trash.createDirectory(name) else trash.createFile("application/octet-stream", name))
                ?: throw IllegalStateException("تعذر إنشاء عنصر في السلة")
            staging = target
            if (source.isDirectory) for (child in source.listFiles()) copyDocumentTree(context, child, target)
            else context.contentResolver.openInputStream(source.uri)?.use { input ->
                context.contentResolver.openOutputStream(target.uri, "w")?.use { output -> input.copyTo(output, 32768) }
                    ?: throw IllegalStateException("تعذرت الكتابة إلى السلة")
            } ?: throw IllegalStateException("تعذرت قراءة الملف")
            if (!source.delete()) throw IllegalStateException("تعذر حذف المصدر بعد النسخ")
            moved++
        } catch (e: Exception) {
            staging?.delete()
            errors += (source.name ?: "item") + ": " + (e.localizedMessage ?: "error")
        }
    }
    return if (errors.isEmpty()) "نُقلت العناصر إلى " + label + ": " + moved else "نُقلت " + moved + " عناصر إلى " + label + "؛ تعذرت بعض العمليات: " + errors.joinToString("; ")
}

private fun restoreFromTrash(context: android.content.Context, sources: List<DocumentFile>, destination: DocumentFile): String {
    var restored = 0
    val errors = mutableListOf<String>()
    for (source in sources) {
        var staging: DocumentFile? = null
        try {
            val name = uniqueDocumentName(destination, source.name ?: "restored-item")
            val target = (if (source.isDirectory) destination.createDirectory(name) else destination.createFile("application/octet-stream", name))
                ?: throw IllegalStateException("تعذر إنشاء العنصر المستعاد")
            staging = target
            if (source.isDirectory) for (child in source.listFiles()) copyDocumentTree(context, child, target)
            else context.contentResolver.openInputStream(source.uri)?.use { input ->
                context.contentResolver.openOutputStream(target.uri, "w")?.use { output -> input.copyTo(output, 32768) }
                    ?: throw IllegalStateException("تعذرت الكتابة إلى الوجهة")
            } ?: throw IllegalStateException("تعذرت قراءة العنصر")
            if (!source.delete()) throw IllegalStateException("تم النسخ لكن تعذر حذف نسخة السلة")
            restored++
        } catch (e: Exception) {
            staging?.delete()
            errors += (source.name ?: "item") + ": " + (e.localizedMessage ?: "error")
        }
    }
    return if (errors.isEmpty()) "تمت استعادة " + restored + " عنصر." else "تمت استعادة " + restored + " عناصر؛ أخطاء: " + errors.joinToString("; ")
}

private fun findDuplicateDocuments(context: android.content.Context, files: List<DocumentFile>): List<DocumentFile> {
    val duplicates = mutableListOf<DocumentFile>()
    val byLength = files.filter { it.length() > 0L }.groupBy { it.length() }
    for (group in byLength.values.filter { it.size > 1 }) {
        val seen = mutableMapOf<String, DocumentFile>()
        for (file in group) {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(file.uri)?.use { input ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            } ?: continue
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (seen.containsKey(hash)) duplicates += file else seen[hash] = file
        }
    }
    return duplicates
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024 && unit < units.lastIndex) { value /= 1024.0; unit++ }
    return String.format(java.util.Locale.ROOT, "%.1f %s", value, units[unit])
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

private fun uniqueArchiveOutputName(parent: DocumentFile, original: String): String {
    val dot = original.lastIndexOf('.')
    val stem = if (dot > 0) original.substring(0, dot) else original
    val extension = if (dot > 0) original.substring(dot) else ""
    var suffix = 1
    while (suffix < 10000) {
        val candidate = stem + "-extracted-" + suffix + extension
        if (parent.findFile(candidate) == null) return candidate
        suffix++
    }
    return System.currentTimeMillis().toString() + "-" + original
}

internal fun extractZipSafely(context: android.content.Context, archiveUri: Uri, destinationUri: Uri): String {
    var extractedFiles = 0
    var extractedBytes = 0L
    var entryCount = 0
    val maxEntries = 10_000
    val maxExtractedBytes = 512L * 1024L * 1024L
    return try {
        val root = DocumentFile.fromTreeUri(context, destinationUri)
            ?: return "تعذر فتح مجلد الحفظ."
        val input = context.contentResolver.openInputStream(archiveUri)
            ?: return "تعذر قراءة الأرشيف المحدد."
        input.use { raw ->
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > maxEntries) {
                        throw IllegalStateException("الأرشيف يحتوي على عدد كبير جداً من العناصر.")
                    }

                    val rawName = entry.name.replace('\\', '/')
                    val parts = rawName.split('/').filter { it.isNotBlank() }
                    if (rawName.startsWith("/") ||
                        rawName.startsWith("\\") ||
                        parts.any { it == ".." || it == "." || it.contains(':') } ||
                        rawName.length > 4096
                    ) {
                        throw IllegalStateException("يحتوي الأرشيف على مسار غير آمن.")
                    }

                    if (parts.isNotEmpty()) {
                        var parent = root
                        for (part in parts.dropLast(1)) {
                            val existingParent = parent.findFile(part)
                            parent = when {
                                existingParent == null -> parent.createDirectory(part)
                                    ?: throw IllegalStateException("تعذر إنشاء مجلد: " + part)
                                existingParent.isDirectory -> existingParent
                                else -> throw IllegalStateException("يوجد ملف في مسار المجلد: " + part)
                            }
                        }

                        val leaf = parts.last()
                        if (entry.isDirectory) {
                            val existingDirectory = parent.findFile(leaf)
                            if (existingDirectory == null && parent.createDirectory(leaf) == null) {
                                throw IllegalStateException("تعذر إنشاء المجلد: " + leaf)
                            }
                            if (existingDirectory != null && !existingDirectory.isDirectory) {
                                throw IllegalStateException("يوجد ملف بالاسم المطلوب للمجلد: " + leaf)
                            }
                        } else {
                            val existing = parent.findFile(leaf)
                            if (existing?.isDirectory == true) {
                                throw IllegalStateException("يوجد مجلد بالاسم المطلوب للملف: " + leaf)
                            }
                            val outputName = if (existing == null) leaf else uniqueArchiveOutputName(parent, leaf)
                            val outputFile = parent.createFile("application/octet-stream", outputName)
                                ?: throw IllegalStateException("تعذر إنشاء الملف: " + outputName)
                            context.contentResolver.openOutputStream(outputFile.uri, "w")?.use { output ->
                                val buffer = ByteArray(32 * 1024)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    extractedBytes += count
                                    if (extractedBytes > maxExtractedBytes) {
                                        throw IllegalStateException("تجاوز حجم الاستخراج الحد المسموح (512 ميغابايت).")
                                    }
                                    output.write(buffer, 0, count)
                                }
                                output.flush()
                            } ?: throw IllegalStateException("تعذر الكتابة إلى الملف: " + leaf)
                            extractedFiles++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        "تم استخراج " + extractedFiles + " ملف بنجاح."
    } catch (e: Exception) {
        "فشل فك الضغط: " + (e.localizedMessage ?: "خطأ غير معروف")
    }
}
