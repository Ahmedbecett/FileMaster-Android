# FileMaster 1.2.0 validation checklist

## CI
- [ ] Gradle configuration succeeds.
- [ ] Debug APK assembles and is uploaded.
- [ ] Release APK is signed with the configured production keystore and verified by apksigner.

## Device checks before public release
- [ ] Confirm battery percentage, voltage and temperature against Android Settings on at least two devices.
- [ ] Confirm RAM and storage readings against system settings.
- [ ] Test thermal status on Android 10+ and confirm unsupported states are labelled honestly.
- [ ] Scan a small folder, a nested folder tree, and a folder granted through the Storage Access Framework.
- [ ] Confirm scan never deletes or modifies files.
- [ ] Confirm drawer navigation and language persistence in Arabic, English, French and Tamazight.
- [ ] Confirm the adaptive launcher icon on the target device.

## Release safety
A successful CI build is not a substitute for physical-device QA. Production signing secrets must be configured in GitHub Actions; never commit the keystore or passwords into this public repository.
