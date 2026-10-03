# Android microphone repair — 1.1.1 (version code 12)

The original APK declared RECORD_AUDIO but omitted MODIFY_AUDIO_SETTINGS. Chromium's AudioManagerAndroid requires both to make recording devices available. This omission is consistent with the reported app-only microphone failure while the camera and website work.

This Android-only update adds that manifest permission, preserves runtime user consent, queues camera and microphone requests, requests only the relevant capture permissions, rechecks the trusted origin before granting, and offers Android app settings after denial. It also repairs the misplaced duplicate installAppExperience Java method that prevented the newer 1.1.0 source from compiling.

Compiled against Android 35 with Java 17, minimum Android 8. APK signature, alignment, package/version and packaged permissions checked. Signing certificate matches the original Xparty-1.0.0.apk, allowing an in-place update from that APK. Actual microphone capture and remote speech on a physical device remain unverified; ask the user to install the update, allow microphone access and test with a second participant.

The web app, playback synchronization and live website are unchanged. Do not use the existing workflow's newly generated signing key for production updates: it changes the app identity on each run. The delivered APK uses the original private key, which is not in this repository.

Reference: https://github.com/chromium/chromium/blob/main/media/base/android/java/src/org/chromium/media/AudioManagerAndroid.java
