# FileMaster Android

**FileMaster** is a native Android file manager and archive utility by **Ahmedbecett**. This repository is being built as a real Android app, not a web mockup.

## Current foundation

- Native Android app using Kotlin and Jetpack Compose.
- Arabic-first interface with a dark theme.
- Android Storage Access Framework for user-selected files and destination folders.
- ZIP extraction into a user-selected folder.
- Basic path traversal checks for archive entries.
- Coroutines keep extraction work off the UI thread.

## Planned development

1. Improve extraction progress, cancellation, overwrite choices, and large-archive handling.
2. Add ZIP creation and archive browsing.
3. Add formats such as 7z, TAR/GZIP, and RAR through suitable libraries, subject to each format's licensing and technical limitations.
4. Build a complete file browser with search, sorting, copy, move, rename, and delete.
5. Add media previews and media-file support.
6. Add tests, accessibility improvements, and release build automation.

## Build

Open the project in Android Studio with JDK 17 and Android SDK 35, then build the `app` module.

## Storage access

FileMaster uses Android's Storage Access Framework so users choose which files and folders the app can access. Android restricts access to certain system and other-app directories; the app does not bypass those protections.

## Maintainer

Ahmedbecett
