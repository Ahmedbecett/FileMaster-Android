# FileMaster Toolkit — implementation plan

Owner/maintainer: Ahmedbecetti  
Support: ahmedbecetti41@gmail.com

This document records the agreed scope for the next release. Items are not considered complete until implemented, built, and tested on a physical Android device.

## Product design
- Replace the launcher icon with a modern FileMaster folder/toolkit mark.
- Refresh the Compose UI with a cohesive contemporary palette, consistent spacing, accessible contrast, and polished cards/buttons.
- Add a dedicated Settings screen.

## Settings and trust
- Language selection: Arabic, English, French, and Tamazight; persist selection and verify RTL layout.
- Recoverable Trash with restore and permanent-delete actions. Use app-managed trash metadata and do not promise recovery for files deleted externally.
- Privacy Policy screen and a public privacy-policy URL before production release.
- Maintainer attribution: Ahmedbecetti.
- Support email: ahmedbecetti41@gmail.com.

## Real utilities (must not be simulated)
- QR scanner using the camera with runtime permission handling; clearly explain when camera access is needed.
- Real Bluetooth file sharing using Android's supported share/transfer intents and permissions. Show the Android system chooser and report failures rather than pretending a transfer succeeded.
- Real Wi-Fi/LAN file transfer between devices only after implementing a secure, explicit session, connection details, transfer progress, and cancellation. Never silently expose files to the network.
- Storage cleanup: identify user-selected temporary/cache/large/duplicate files and ask for confirmation before deletion. Do not label arbitrary files as malware or promise antivirus detection.
- Device status: show available memory/RAM information where Android permits it, CPU/device information with clear limits, and network connectivity/link speed only when actually available from Android APIs. Do not claim a live ISP speed test unless one is implemented and consented to.

## Existing file-management scope to preserve
- SAF-based file browsing, favorites/recent items, search, copy/move/rename/delete, duplicate scanning, ZIP creation/extraction, and AdMob integration.
- Consider RAR/7z/TAR/GZIP support only after choosing compatible libraries and testing archive path safety.

## Release gates
- Build and lint successfully in CI.
- Test on a real Android phone: language persistence and RTL, SAF providers, deletion/recovery, QR permission denial, Bluetooth cancellation/failure, Wi-Fi session isolation, cleanup confirmation, and device/network status accuracy.
- Review AdMob consent/privacy disclosures and configure production signing. A successful debug build alone is not a production release.
