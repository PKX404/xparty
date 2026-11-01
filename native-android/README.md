# XParty v24 -> Native Android Port (Preview)

This Android-first prototype recreates XParty v24's home, room, source chooser, YouTube browsing, room chat and playback layout using native Android Views and a fixed-size Media3 ExoPlayer stage. The **only WebViews** are YouTube's browsing and official embedded player, where provider web integration is appropriate.

- Backend: `https://xp404.onrender.com/ws` native OkHttp WebSocket, XParty create/join/welcome/state/chat/playback messages. Backend can be changed in Room settings.
- Android playback: Media3 ExoPlayer for HTTPS MP4/HLS/IPTV and local selected video; official embed for YouTube.
- Native chat and inline long-press actions.
- No playback controls when no source is selected; fixed video height avoids keyboard resizing.
- Android package: `com.pkx404.xparty.nativepreview`, separate from Test17.
- Build: `gradle --no-daemon :app:assembleDebug` with Gradle 8.14.3, JDK17, Android SDK 36.

**Not feature complete:** microphone/camera calls, login, synced upload of local files, production-grade YouTube synchronization, channel directory, message receipts, reactions, full moderation, notification system and offline reconnection hardening need further work. WebView YouTube may restrict login/playback. No guarantee zero buffering. The native UI is an independent implementation and does not copy Rave or Teleparty.

This is a test APK, not a Play Store release. Before launch replace debug signing with a permanent release key, validate socket protocol against live server, and test across Android physical devices.