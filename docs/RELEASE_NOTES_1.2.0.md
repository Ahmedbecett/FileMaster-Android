# FileMaster 1.2.0 — Release notes

## Interface and identity
- Updated the app palette to deep navy and teal with clearer contrast and consistent sans-serif typography.
- Replaced the launcher icon with a FileMaster folder-and-document mark.
- Added a branded slide-out navigation drawer.
- Moved language selection out of the top bar and into Settings; the selector adapts to narrow screens.

## Real device diagnostics
- Battery percentage, charging state, health field, voltage and battery temperature where reported by Android.
- System RAM availability and data-partition storage availability.
- Android thermal status where supported, with safe cooling guidance.
- Animated battery gauge and refresh action.

## Real file scan
- Recursively enumerates files and folders in the user-selected Storage Access Framework directory.
- Reports actual file counts, aggregate reported size, largest files and common temporary-file candidates.
- Does not delete files automatically. Results depend on access granted by the selected storage provider.

## Platform limitations
- Third-party Android apps generally cannot turn on or control a built-in phone cooling fan.
- Thermal status and battery temperature depend on the device/OEM; unsupported readings are reported as unavailable.
- Device testing on multiple physical phones is still required before declaring every OEM behavior verified.

Version: 1.2.0 (versionCode 3)
Maintainer: Ahmedbecetti
