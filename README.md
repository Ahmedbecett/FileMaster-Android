# FileMaster Android

**FileMaster** is a native Android file manager and archive utility by **Ahmedbecett**. It is built with Kotlin and Jetpack Compose; it is not a web mockup.

> **🚧 Work in progress — not a final release.** FileMaster is still under active development. Features, compatibility, and stability may change. Please report issues and test carefully before using it with important files.

## Download the Android APK

**[⬇️ Download the latest successful debug APK from GitHub Actions](https://github.com/Ahmedbecett/FileMaster-Android/releases/latest/download/app-debug.apk)**

The link downloads the APK directly from the latest GitHub Release. After downloading, open the APK on your Android phone and follow the installation prompts. Android may ask you to allow installation from that source.

> **Download note:** This is a development/debug build, not a production-ready release. The stable link above points to the APK attached to the latest GitHub Release. The workflow publishes a release automatically after a successful build on the `main` branch; the link will work once that release has been created.

## Features currently implemented

- **Four-language interface in progress:** Arabic, English, French, and Tamazight (Tifinagh). Translation coverage and layout still need testing and improvement.
- **File browsing:** Browse folders and files using Android's Storage Access Framework.
- **Search and navigation:** Search within the selected folder, navigate folders, and paginate through large folders.
- **Favorites:** Star files and folders; favorites are saved locally.
- **Recent files:** Remember a small local list of recently opened files.
- **Folder overview:** Show the number of direct children and reported file sizes in the selected folder.
- **Quick filters:** Filter by favorites, common image formats, recently opened items, or files at least 10 MiB.
- **Duplicate scan:** Compare same-size files using SHA-256 to find byte-identical duplicates; scanning never deletes files.
- **Saved language:** Remember the selected interface language between launches.
- **File information:** View file names and sizes.
- **Folder management:** Create folders, rename items, and delete selected items.
- **Copy and move:** Copy or move selected items to a user-selected destination.
- **Multi-format archive extraction:** Extract ZIP, 7Z, RAR, TAR, TAR.GZ/TGZ and single-file GZIP archives, with path checks and entry/expanded-size limits. Extraction needs testing against real archives and Android document providers.
- **Archive preview:** List archive entries before extraction (up to the first 100 displayed), plus existing ZIP archive creation.
- **ZIP creation:** Create ZIP archives from selected files and folders.
- **Open files:** Open supported documents and media using compatible apps installed on Android.
- **In-app image preview:** Preview JPG, JPEG, PNG, WebP, BMP and GIF still frames inside the app.
- **Recoverable Trash:** Move selected items into an app-managed Trash folder, restore selected items, or permanently delete items from Trash. The trash is stored inside the browsed folder and is not a system-wide recycle bin.
- **Hide/unhide selected items:** Move chosen files or folders into a dot-prefixed .FileMasterHidden folder and restore them from the hidden-items view. This hides them from FileMaster's normal list but does not encrypt them or prevent other file managers from accessing them.
- **PIN privacy lock:** Optional numeric PIN gates the app interface. It does not encrypt the underlying files, and this is not a substitute for Android device security.
- **QR/barcode scanner:** Uses Google Play services Code Scanner.
- **Device information:** Shows total and currently available RAM. It does not currently report live CPU percentage or a measured internet-speed test.
- **Safe junk scan:** Flags common temporary/log/backup-download file extensions in the currently selected folder for review. It never auto-deletes; using Delete sends selected candidates to the app-managed Trash.
- **Sharing:** Send selected files through Android's share sheet to installed Bluetooth/Wi-Fi apps, or start a temporary read-only HTTP server on the local network to let nearby devices download selected files using a browser. Stop LAN sharing when finished; anyone with the URL on that network can access the selected files. Built-in FTP is not implemented.
- **Settings and privacy summary:** Includes language controls, developer contact, and a privacy-policy summary. A complete public legal policy and ad-consent flow are still needed before production release.
- **Android-native app:** Kotlin + Jetpack Compose, with FileMaster branding and launcher icon.
- **Automated builds:** GitHub Actions builds a debug APK and publishes it as an artifact.

## Advertising (Google AdMob)

- Google Mobile Ads SDK is integrated with a banner placement in the app.
- AdMob app ID: configured in `AndroidManifest.xml`.
- Banner ad unit: configured in the app's Compose UI.
- Ads require internet access and may not appear until the AdMob account, app, and ad unit are approved and serving. Follow Google's consent and privacy requirements for your audience and region.

## Limitations and development status

- **Archive coverage:** Multi-format extraction has been added on the feature branch, but format compatibility, password-protected archives, malformed archives, and large files still require testing. ZIP creation is currently the archive-creation format.
- **Folder scope:** Overview, quick filters, and duplicate scanning operate on the selected folder's immediate children, not a full-device index. Directory sizes depend on the Android document provider.
- **Trash and recovery:** App-managed Trash is implemented on the feature branch; it depends on storage-provider permissions and should be tested before relying on it for important files.
- **Privacy lock and local-network transfer:** The PIN lock is available. LAN sharing is a temporary read-only server, not FTP; it requires device testing on real Wi-Fi networks.
- **Integrated previews:** Still-image preview is available on the feature branch. Built-in audio, video and PDF previews are not implemented.
- **Themes and translations:** Light/dark theme choice is saved locally. The four language options have been started, but complete translation coverage, correct right-to-left layout for Arabic, and Tamazight display need real-device testing.
- **Testing:** Copy/move and archive operations need broader testing across Android versions, document providers, and large files.
- **Protected folders:** Access to Android system-protected directories is controlled by Android. FileMaster does not bypass these restrictions.
- **Debug build:** A successful build confirms compilation, not that every feature has been fully tested on a physical device.

## Roadmap

1. Verify the latest build and test favorites, recent history, filters, and duplicate scanning on a real Android device.
2. Complete Arabic, English, French, and Tamazight translations and right-to-left layout.
3. Test multi-format archive extraction thoroughly, including password-protected archives, malicious paths, and large files; add overwrite prompts and archive creation beyond ZIP.
4. Add operation progress/cancellation and more robust rollback/cleanup after failed extraction or interrupted Trash operations.
5. Extend in-app previews to audio, video, and PDF/document files.
6. Review LAN-sharing security and add optional authenticated transfer; add FTP only if it can be implemented and tested safely.
7. Add automated tests and prepare a signed production release with privacy disclosures and ad consent handling.

## Build from source

The GitHub Actions workflow installs JDK 17, Gradle 8.11.1 and Android SDK 35, then runs `gradle --no-daemon assembleDebug`. Open the project in Android Studio with JDK 17 and Android SDK 35 to build locally.

## Maintainer

**Ahmedbecett**
