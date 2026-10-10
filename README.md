# FileMaster Android

**FileMaster** is a native Android file manager and archive utility by **Ahmedbecett**. It is built with Kotlin and Jetpack Compose; it is not a web mockup.

> **🚧 Work in progress — not a final release.** FileMaster is still under active development. Features, compatibility, and stability may change. Please report issues and test carefully before using it with important files.

## Download the Android APK

**[⬇️ Download the latest successful debug APK from GitHub Actions](https://github.com/Ahmedbecett/FileMaster-Android/actions/runs/38013713808)**

Open the workflow run, scroll to **Artifacts**, and download **FileMaster-debug-apk**. Extract the ZIP archive to get the APK, then install it on your Android phone. Android may ask you to allow installation from that source.

> **Download note:** This is a development/debug build, not a production release. GitHub Actions artifacts are temporary and may expire; the link above points to the verified build available at the time this README was updated. A permanent APK link will be added with a GitHub Release.

## Features currently implemented

- **Four-language interface in progress:** Arabic, English, French, and Tamazight (Tifinagh). Translation coverage and layout still need testing and improvement.
- **File browsing:** Browse folders and files using Android's Storage Access Framework.
- **Search and navigation:** Search within the selected folder, navigate folders, and paginate through large folders.
- **File information:** View file names and sizes.
- **Folder management:** Create folders, rename items, and delete selected items.
- **Copy and move:** Copy or move selected items to a user-selected destination.
- **ZIP extraction:** Extract ZIP archives to a chosen folder with basic path-traversal checks and limits on entry count and extracted bytes.
- **ZIP creation:** Create ZIP archives from selected files and folders.
- **Open files:** Open supported documents and media using compatible apps installed on Android.
- **Android-native app:** Kotlin + Jetpack Compose, with FileMaster branding and launcher icon.
- **Automated builds:** GitHub Actions builds a debug APK and publishes it as an artifact.

## Limitations and development status

- **ZIP only:** RAR, 7z, TAR and GZIP are not implemented yet.
- **Translations:** The four language options have been started, but complete translation coverage, correct right-to-left layout for Arabic, and Tamazight display need real-device testing.
- **Testing:** Copy/move and archive operations need broader testing across Android versions, document providers, and large files.
- **Protected folders:** Access to Android system-protected directories is controlled by Android. FileMaster does not bypass these restrictions.
- **Debug build:** A successful build confirms compilation, not that every feature has been fully tested on a physical device.

## Roadmap

1. Verify builds and fix any CI or runtime issues; add automated tests for archive extraction and file operations.
2. Finish and review Arabic, English, French, and Tamazight translations and layout behavior.
3. Improve overwrite handling, progress reporting, cancellation, and large-file handling.
4. Add RAR, 7z, TAR and GZIP support using suitable libraries after checking licenses and compatibility.
5. Add previews for images, audio, video and documents.
6. Improve accessibility, sorting, multi-select UX, and prepare a signed production release.

## Build from source

The GitHub Actions workflow installs JDK 17, Gradle 8.11.1 and Android SDK 35, then runs `gradle --no-daemon assembleDebug`. Open the project in Android Studio with JDK 17 and Android SDK 35 to build locally.

## Maintainer

**Ahmedbecett**
