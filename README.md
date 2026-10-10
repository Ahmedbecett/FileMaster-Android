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
- **Recoverable Trash (new on the feature branch):** Selected files and folders are copied into a .FileMasterTrash folder inside the currently browsed folder before the originals are removed; selected items can be restored, with collision-safe restored names. Permanent deletion is available from inside Trash. This depends on the selected Android document provider allowing create, read, write, and delete operations.
- **Built-in image preview (new on the feature branch):** Preview common JPG, JPEG, PNG, WebP, BMP and GIF image files in an in-app dialog. GIF preview displays the decoded still frame, not animation.
- **Folder management:** Create folders, rename items, and delete selected items.
- **Copy and move:** Copy or move selected items to a user-selected destination.
- **ZIP extraction:** Extract ZIP archives to a chosen folder with basic path-traversal checks and limits on entry count and extracted bytes.
- **ZIP creation:** Create ZIP archives from selected files and folders.
- **Open files:** Open supported documents and media using compatible apps installed on Android.
- **Android-native app:** Kotlin + Jetpack Compose, with FileMaster branding and launcher icon.
- **Automated builds:** GitHub Actions builds a debug APK and publishes it as an artifact.

## Advertising (Google AdMob)

- Google Mobile Ads SDK is integrated with a banner placement in the app.
- AdMob app ID: configured in `AndroidManifest.xml`.
- Banner ad unit: configured in the app's Compose UI.
- Ads require internet access and may not appear until the AdMob account, app, and ad unit are approved and serving. Follow Google's consent and privacy requirements for your audience and region.

## Limitations and development status

- **ZIP only:** RAR, 7z, TAR and GZIP are not implemented yet.
- **Folder scope:** Overview, quick filters, and duplicate scanning operate on the selected folder's immediate children, not a full-device index. Directory sizes depend on the Android document provider.
- **Trash and recovery:** The feature branch now adds a recoverable app-managed Trash for the selected folder. It is not a system-wide recycle bin, and restoration must be tested with each Android document provider.
- **Privacy lock and local-network transfer:** Not implemented yet.
- **Integrated previews:** Common still images now have an in-app preview on the feature branch. Audio, video and PDF files still open with compatible installed apps; built-in audio/video/PDF preview is not implemented yet.
- **Translations:** The four language options have been started, but complete translation coverage, correct right-to-left layout for Arabic, and Tamazight display need real-device testing.
- **Testing:** Copy/move and archive operations need broader testing across Android versions, document providers, and large files.
- **Protected folders:** Access to Android system-protected directories is controlled by Android. FileMaster does not bypass these restrictions.
- **Debug build:** A successful build confirms compilation, not that every feature has been fully tested on a physical device.

## Roadmap

1. Build and test the feature branch on a real Android device, especially Trash/restore and image previews across document providers.
2. Complete Arabic, English, French, and Tamazight translations and right-to-left layout.
3. Add tested RAR, 7z, TAR and GZIP support with safe extraction limits.
4. Add overwrite prompts, operation progress/cancellation, and robust cleanup after failed extraction.
5. Extend in-app previews to audio, video, and PDF/document files.
6. Consider an optional privacy lock and secure local-network transfer after permissions and security review.
7. Add automated tests and prepare a signed production release with privacy disclosures and ad consent handling.

## Build from source

The GitHub Actions workflow installs JDK 17, Gradle 8.11.1 and Android SDK 35, then runs `gradle --no-daemon assembleDebug`. Open the project in Android Studio with JDK 17 and Android SDK 35 to build locally.

## Maintainer

**Ahmedbecett**
