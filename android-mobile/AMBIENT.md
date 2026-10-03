# Ambient background — Android development checkpoint

This module is integrated into the bundled Android client, not the website. The published APK/update manifest remains at 2.0.0 until a new signed release is built and validated.

- Local files: sample the existing video into one 32×18 canvas, at most once every 800 ms. Repeated frame timestamps are skipped. No extra media decoder, playback commands, seeks or audio processing.
- YouTube: load one CORS-enabled thumbnail per source activation; use a neutral palette if loading, CORS or canvas access fails. This is artwork-adaptive, not video-frame-adaptive.
- Auto: three soft masked colour regions, with slow transform movement on devices without low-resource hints. Soft: no movement and lower opacity. Off: stops sampling, pending images and timers immediately, without reloading or disconnecting the call.
- Hidden/inactive rooms suspend work. Thumbnail callbacks are invalidated on source changes. Image requests time out after eight seconds. Reduced motion is honored. Slow sampling reduces its frequency.
- Optional setting persistence follows preference consent. Revoking it removes the persistent ambient setting.

Run `node android-mobile/tests/ambient.mjs`. Tests cover palette extraction, avoiding a second decoder, duplicate frames, suspension, Off, reduced motion, tainted canvas, stale artwork and disposal. These mocked lifecycle tests do not measure Android GPU use, real network playback or call quality.

Before release: inspect the background and settings on real Android WebView; compare local playback dropped-frame deltas, call packet loss/jitter and sampling costs with Auto/Soft/Off on the same devices and networks. Lifetime dropped-frame ratios cannot reliably attribute regressions to this effect. Do not promise zero lag or a fixed performance improvement without measurements.
