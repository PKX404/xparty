# Room redesign review

The background now fills the viewport behind video, chat, participants and margins. Immersive and Live background controls are in a persistent room toolbar. Watch sources opens the provider picker separately; selecting a source closes it so mobile chat stays closer to playback. The participant sheet provides photos, status, search, profile access and the existing moderation controls. Chat has explicit white text and dark translucent surfaces across themes.

## Frame rendering

Files and CORS-enabled direct links use the same decoded video element as the player. requestVideoFrameCallback drives rendering with one outstanding callback and no frame backlog; older browsers use animation frames and skip unchanged media times. There is no four-Hz render timer or CSS color interpolation. A one-second watchdog only checks lifecycle state.

The independent WebGL renderer downsizes to 256 × 144 and uses two Gaussian passes, pairing adjacent taps through bilinear filtering for 13 texture reads per pass instead of 25. Texture storage is reused and normal rendering avoids CPU pixel readback. Final grading uses saturation 1.5 and a 0.75 luma ceiling. Unsupported WebGL falls back to a small palette at at most 10 Hz. Pause and reduced motion retain a still frame; hidden room/document and disabled state stop rendering. Context restoration rebuilds GPU resources.

YouTube artwork is a static fallback. Supported browsers can supply moving iframe colors only after explicit current-tab capture selection. Exact capture handle/origin checks reject other tabs/screens; no audio is captured. The capture request is 30 fps ideal, 60 maximum, but browser delivery and delay vary. Netflix/Prime companion tabs do not supply protected pixels. Mobile WebView capture is not implemented.

The supplied Rave decompiled shaders and participant layouts were inspected read-only. They informed blur/grading and avatar/name/action layout choices. No proprietary implementation/assets were copied; no runtime Rave comparison was available. Native SurfaceTexture behavior, dithering, exact output and complete Rave feature parity are not claimed.

## Providers

| Button | Implemented path | Boundary |
|---|---|---|
| YouTube | Existing official embedded player/search | Availability/embed permissions apply |
| Vimeo | Official embedded player | Public embeddable video |
| Twitch | Official channel/VOD player | Live edge timing differs; no live seeking; minimum 400 × 300, horizontally scrollable on narrow screens |
| SoundCloud | Official widget with millisecond/second conversion | Public single tracks only |
| Netflix / Prime Video | Optional desktop extension pairs each user's own signed-in service tab with the room timeline | Preview; actual signed-in playback unverified; no embedded/mobile/DRM frame access |
| Google Drive | Existing protected account library route | Google account/provider configuration required |
| Video link / Your files | Native playback | CORS/codec support or temporary file transfer |

The requested all-service parity is not complete: Disney+, HBO Max, Crunchyroll, Tubi, Pluto TV, X and Google Photos still need separate integrations and provider validation. These appear in [Rave's current service list](https://rave.io/faq); no working integration is claimed here for them.

The companion validates origins, tab/document/source IDs and random pairing nonces. It exchanges bounded playback state, never room tokens, account sessions, cookies or chat. Provider user actions return through existing host/approval/controller rules. It disconnects on stale pair/room/source and suppresses command feedback. Installation, known advert/episode limits and the exact host allowlist are in [the companion README](../browser-extension/README.md).

## Verification and limits

Frame-pump tests check scheduling, cancellation and duplicate-frame suppression. Real WebSocket tests cover the new providers, canonical URLs, host approval, unauthorized playback, queue transitions, live buffer behavior and extension download. Provider SDK and extension APIs are simulated in focused tests; those do not establish actual service support.

The isolated Chrome browser suite decodes MDN's CC0 MP4, checks frame updates, full-viewport bounds, white chat text, pause, reduced motion, participant actions and provider UI. The final software-GPU run produced 29 updates in 1.4 seconds, p95 render cost 3.9 ms and maximum observed media timestamp difference 0.042 seconds. All 73 Node tests and the three browser suites passed. These are local measurements, not hardware frame-rate or end-to-end latency promises. GPU checks cap white at RGB 191, preserve spatial red/blue regions and report no GL errors. Viewports checked: 320, 390, 844 and 1366 pixels.

Browser suites: `tests/redesign-browser.cjs`, `tests/immersive-room-browser.cjs`, `tests/auth-browser.cjs`. Set PLAYWRIGHT_MODULE and CHROMIUM_PATH to installed Playwright/Chrome. The immersive suite downloads the [MDN CC0 fixture](https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4); TEST_VIDEO_PATH supports an offline copy. Installed extension lifecycle, actual provider accounts, real current-tab capture and physical Android remain unverified. See [schema and deployment findings](SCHEMA-RESULTS-20261022.md); no migration or deployment was performed.

Technical references: [video-frame callbacks](https://developer.mozilla.org/en-US/docs/Web/API/HTMLVideoElement/requestVideoFrameCallback), [Twitch embedding](https://dev.twitch.tv/docs/embed/video-and-clips/), [SoundCloud widget](https://developers.soundcloud.com/docs/api/html5-widget).
