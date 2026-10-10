# FileMaster Android

**FileMaster** is a native Android file manager and archive utility by **Ahmedbecett**. It is built with Kotlin and Jetpack Compose; it is not a web mockup.

## Implemented in the current development build

- Arabic-first dark interface.
- Android Storage Access Framework folder picker, scoped to folders the user chooses.
- Browse folders and files, search within the displayed folder, and view file sizes.
- Create folders, rename items, and delete selected items.
- Copy or move selected items to a user-selected destination.
- Extract ZIP archives into a user-selected destination, with basic archive path traversal checks and limits on entry count and extracted bytes.
- Create ZIP archives from selected files and folders.
- GitHub Actions workflow to build a debug APK and upload it as an artifact.

## Important limitations

- The current archive engine supports ZIP only. RAR, 7z, TAR and GZIP are planned and are not implemented yet.
- Copy/move and archive operations should be tested on different Android document providers and with large files before production release.
- Access to Android system-protected directories is controlled by Android; FileMaster does not bypass those restrictions.
- The APK is not considered verified until the GitHub Actions build succeeds and the APK is installed and tested on a real device.

## Build

The GitHub Actions workflow installs JDK 17, Gradle 8.11.1 and Android SDK 35, then runs `gradle --no-daemon assembleDebug`. Open the project in Android Studio with JDK 17 and Android SDK 35 to build locally.

## Roadmap

1. Verify and fix the CI build; add automated tests for archive extraction and file operations.
2. Improve overwrite handling, progress, cancellation, and very large archive support.
3. Add RAR, 7z, TAR and GZIP through suitable libraries after checking licenses and compatibility.
4. Add previews for images, audio, video and documents.
5. Improve accessibility, sorting, multi-select UX and release packaging.

## Maintainer

Ahmedbecett
