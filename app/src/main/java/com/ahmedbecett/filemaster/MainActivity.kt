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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
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

private val Ink = Color(0xFF101522)
private val Panel = Color(0xFF1B2434)
private val Accent = Color(0xFF79E0C3)
private val Muted = Color(0xFFADB8C8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
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
    var qrResult by remember { mutableStateOf<String?>(null) }
    fun tr(text: String): String = fileMasterText(text, language)
    val scope = rememberCoroutineScope()
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
            browsePage = 0
            browseFiles = browseFolder?.listFiles()?.sortedWith(
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
                            Text(tr("مدير الملفات والأرشيفات"), style = MaterialTheme.typography.labelMedium, color = Muted)
                        }
                    },
                    actions = {
                        IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Default.Settings, contentDescription = tr("الإعدادات"), tint = Accent) }
                        Box {
                            TextButton(onClick = { languageMenuOpen = true }) { Text(tr("اللغة") + ": " + fileMasterLanguageName(language), color = Accent) }
                            DropdownMenu(expanded = languageMenuOpen, onDismissRequest = { languageMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("العربية") }, onClick = { language = "ar"; preferences.edit().putString("language", language).apply(); languageMenuOpen = false })
                                DropdownMenuItem(text = { Text("English") }, onClick = { language = "en"; preferences.edit().putString("language", language).apply(); languageMenuOpen = false })
                                DropdownMenuItem(text = { Text("Français") }, onClick = { language = "fr"; preferences.edit().putString("language", language).apply(); languageMenuOpen = false })
                                DropdownMenuItem(text = { Text("ⵜⴰⵎⴰⵣⵉⵖⵜ") }, onClick = { language = "tz"; preferences.edit().putString("language", language).apply(); languageMenuOpen = false })
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
                                    val root = browseFolder
                                    val parent = root?.parentFile
                                    if (parent != null) {
                                        browsePage = 0
                                        browseFolder = parent
                                        browseFiles = parent.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                        zipSelection = emptySet()
                                    }
                                }) { Text(tr("المجلد السابق")) }
                                OutlinedButton(onClick = {
                                    destination = browseRootUri
                                    status = tr("تم تعيين المجلد الرئيسي المختار كوجهة لفك الضغط.")
                                    success = true
                                }) { Text(tr("استخدام كوجهة")) }
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
                                    Text("Folder overview", fontWeight = FontWeight.Bold, color = Accent)
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
                                                browseFiles = file.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() })
                                                zipSelection = emptySet()
                                            } else {
                                                val fileName = file.name ?: "file"
                                                recentUris = (listOf(file.uri.toString()) + recentUris.filterNot { it == file.uri.toString() }).take(20)
                                                preferences.edit().putString("recent", recentUris.joinToString("\n")).apply()
                                                if (fileName.lowercase().endsWith(".zip")) {
                                                    selectedZip = file.uri
                                                    selectedName = fileName
                                                    status = tr("تم اختيار ") + fileName + tr(". استخدم زر فك الضغط لاستخراجه.")
                                                    success = true
                                                } else {
                                                    val extension = fileName.substringAfterLast('.', "").lowercase()
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
                    title = tr("اختيار أرشيف ZIP"),
                    detail = selectedName ?: tr("اختر ملفاً من ذاكرة الهاتف"),
                    buttonText = tr("تصفح الملفات"),
                    onClick = { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")) }
                )
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
                            status = tr("اختر ملف ZIP أولاً.")
                            success = false
                        } else if (target == null) {
                            status = tr("اختر مجلد الحفظ أولاً.")
                            success = false
                        } else {
                            busy = true
                            success = false
                            status = tr("جارٍ فك الضغط…")
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { extractZipSafely(context, archive, target) }
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
                        OutlinedButton(onClick = { settingsOpen = false; privacyOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("سياسة الخصوصية")
                        }
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
                onDismissRequest = { deviceInfoOpen = false },
                title = { Text("معلومات الجهاز") },
                text = { Text("الذاكرة الكلية: " + formatFileSize(mi.totalMem) + "\nالذاكرة المتاحة حالياً: " + formatFileSize(mi.availMem) + "\nالذاكرة المستخدمة تقريبياً: " + formatFileSize((mi.totalMem - mi.availMem).coerceAtLeast(0)) + "\n\nلا تعرض هذه الشاشة نسبة CPU أو سرعة الإنترنت الحية؛ يلزم تنفيذ قياس مستقل موثوق لهما.") },
                confirmButton = { TextButton(onClick = { deviceInfoOpen = false }) { Text("إغلاق") } }
            )
        }
        if (managerDialog == "delete") {
            AlertDialog(
                onDismissRequest = { managerDialog = "" },
                title = { Text(tr("حذف العناصر المحددة؟")) },
                text = { Text(tr("سيتم حذف ") + zipSelection.size + tr(" عنصر. قد لا يمكن التراجع عن ذلك.")) },
                confirmButton = {
                    TextButton(onClick = {
                        val targets = browseFiles.filter { it.uri.toString() in zipSelection }
                        val removed = targets.count { it.delete() }
                        status = tr("تم حذف ") + removed + tr(" من ") + targets.size + tr(" عنصر.")
                        managerDialog = ""
                        zipSelection = emptySet()
                        browseFiles = browseFolder?.listFiles()?.sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }) ?: emptyList()
                    }) { Text(tr("حذف")) }
                },
                dismissButton = { TextButton(onClick = { managerDialog = "" }) { Text(tr("إلغاء")) } }
            )
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

private fun extractZipSafely(context: android.content.Context, archiveUri: Uri, destinationUri: Uri): String {
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
