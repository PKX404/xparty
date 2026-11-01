# XParty Android Native Preview

Built with the Android SDK 36 and Gradle 8.14.3; Android Gradle Plugin 8.11.2 and Java 17.

This is a **standalone installable preview**, not the Test 17 watch-party app.
It has native Android room/chat screens, an in-app mobile YouTube browser,
an official YouTube embed for selected videos, and Android VideoView for compatible HTTPS streams.

It does **not** include Socket.IO, backend room sync, authentication, calls,
moderation, profiles, or production signing yet. YouTube website and embed
behavior is subject to YouTube's WebView/platform restrictions.

To build with Android Studio open `native-android/` or use the GitHub Actions
workflow `XParty Native Android APK (Gradle + SDK)`.
Download the artifact, extract `app-debug.apk`, and install it.
Package `com.pkx404.xparty.nativepreview` keeps older XParty versions untouched.
