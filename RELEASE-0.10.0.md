# Xparty 0.10.0

Core room reliability and interface release.

- Read-only Force sync for every member; buffering leases recover stale holds.
- Automatic ICE/signaling recovery, with manual reconnect override.
- Host-enabled/approved PTT requests from any room member; four call seats remain enforced.
- Wake-lock lifecycle, status icons/crown, compact call actions, internal selectors, room-size slider and labeled view controls.
- Distinct friend-message navigation; pending approvals above chat.
- Grouped sound settings, notification volume/master toggle, light/dark Theme, theatre Fit/Fill and expanded Hindi/Kannada labels.

Validation: 15 server tests and syntax checks pass. Local browser interface/simulated-YouTube tests pass at four viewport widths. Actual media transport is blocked in the test environment by zero gathered ICE candidates and is not certified.

Not enabled: cloud/virtual browser, Netflix/Prime, Google/OTP/cross-room accounts without provider setup, and TURN without relay configuration. See docs/IMPROVEMENT-MAP-20261001.md for the full request map and outstanding work.
