# Xparty Mobile 2.0

Independent Android client project. Its interface and JavaScript are bundled in the APK under `app/src/main/assets/client`; the existing `xparty-service/public` website is not modified or deployed. It shares the current HTTPS room API/WebSocket backend and room codes at pkxparty.onrender.com. This is a packaged hybrid Android client, not a Kotlin/Compose rewrite or a separate backend.

## Implemented request checklist

- Separate mobile project and release channel: `android-mobile` branch, `android-mobile/` build root.
- App-specific Watch / Call / Chat / Browse bottom navigation; compact top controls; lime branding and Made4Love dedication.
- Repeated horizontal swipe Call ↔ Chat without replacing media tracks; vertical scrolling still works.
- Host Play now and approved video selections start the room timeline without a second Play press. Playback clocks and drift correction remain unchanged.
- Search has a 12-second timeout, cancels previous requests, ignores stale results, and dismisses the keyboard on explicit search/selection. No replacement of the focused input.
- Final approval state clears stale Accept/Decline buttons.
- Smaller PiP button; explicit Android PiP remains.
- BGM / Talk prioritizes call/chat with a visible compact YouTube player. Hidden YouTube/audio extraction and guaranteed screen-off playback are not implemented.
- Weak-network call video adapts using candidate RTT, bandwidth and packet-loss deltas with recovery hysteresis; Voice focus explicitly stops the camera while retaining microphone/chat. No volume amplification.
- Top-bar guide; Settings and guide expose Check for app update.
- Update check reads a release manifest from this app branch, compares versionCode, and offers the signed APK download through Android's browser/installer. Android approval remains required; no silent installation.

## Remaining limits and acceptance gates

- Real two-device tests on different mobile/Wi-Fi networks, real YouTube buffering and actual microphone quality need device verification. Synthetic test streams do not establish speech quality.
- No promise of zero buffering or call quality better than every other app. YouTube controls its streaming quality; poor connectivity and relay availability affect calls. The existing server supplies STUN/TURN settings; this app does not provision TURN or an SFU.
- This is an app-specific hybrid redesign, not yet an entirely native UI/media stack.
- Native update dialog/device installation and Bluetooth routing require phone tests.

## Build and release

Use `build-apk.sh` with Android 35 build tools, Java 17, and the existing private release key via environment variables. Never commit signing credentials. The output updates com.pkx404.xparty in place.

Publish each signed APK under `android-mobile/releases/` on the `android-mobile` branch and update `/releases/android.json` in the same commit. Increment both manifest versionCode/versionName and the native update comparison for every release. Verify signatures before publication. The website branch/deployment is not part of this release.

## Validation

`tests/mobile.cjs` runs against an isolated Xparty server on loopback. It tests two-client room entry, host autoplay and accepted-request autoplay with a mock YouTube player, clears approval actions, verifies inbound synthetic WebRTC audio, repeated swipes preserving mic, layout overflow, guide, and update link. It must not be described as real YouTube/network/device validation.

Latest validation additionally passed search timeout recovery (busy state cleared) and no horizontal page overflow at 320, 768 and 1024 CSS pixels. Thumbnail fetches in the local test environment had certificate failures; these were not counted as successful real YouTube access.
