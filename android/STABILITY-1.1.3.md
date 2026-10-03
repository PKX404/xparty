# Android 1.1.3 stability correction

- Remove synthetic online events on Activity resume. The web client treats these as forced ICE recovery even for connected calls; permission/app returns must not trigger that path.
- Physical volume keys select the communication stream while Android is in communication/call mode, otherwise the media stream. No automatic volume increase, gain amplification, or forced output route.
- Keep PiP available explicitly; remove automatic PiP entry on Activity leave to avoid unwanted call layout changes.
- Disable costly backdrop filters on live-call overlays and conversation entrance animation in the APK only. Preserve all dimensions.
- Retain 1.1.1 microphone permission fix and 1.1.2 compact layout restoration.

Validation: compile against Android 35; APK signing/alignment and manifest check. No changes to website, WebRTC signaling, or playback synchronization source. Physical device audio/video quality, Bluetooth routing, and perceived rendering smoothness remain unverified. This is a targeted correction, not a guarantee that network packet loss, echo, or distorted sound is fully resolved.
