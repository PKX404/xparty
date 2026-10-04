# Xparty 2.1.1 — Google account access

Version code 23; package com.pkx404.xparty. Signed using the existing release certificate, so Android can upgrade the previous APK without removing app data.

- Google OAuth opens the system browser and returns via xparty://login-callback.
- Supabase PKCE exchanges the one-time code in the original app; no Google client secret is bundled.
- Android Keystore AES-GCM encrypts Supabase sessions and PKCE verifier. Existing WebView auth state migrates on first launch. Bridge uses a top-frame, exact-origin message port rather than an all-frame JavaScript interface.
- Google is above email OTP. Email and guest paths retained. No sync/player/WebRTC engine changes.
- Facebook and Instagram deferred.

## Required Supabase setting
Authentication → URL Configuration → Redirect URLs must include the exact `xparty://login-callback`. Keep existing web URLs. This allowlist has not been independently verified in the authenticated dashboard.

Google enabled and client ID/callback checked through the public authorization endpoint. Client secret exchange and actual user sign-in still require a device test.

## Verification
Java compilation, APK v2/v3 signatures, 16KB zip alignment, JS syntax, five Node auth-flow tests passed. Signing SHA-256: 62095963af7fc8d2e88012638a9613f12ccd04a1a95925239cda335ff363be92.

## Device acceptance checks (pending)
1. Install over existing app. Confirm guest room, microphone, camera and synchronized playback.
2. Accept privacy acknowledgment; Continue with Google → choose account → return to Xparty.
3. Existing verified-email user must keep the same Supabase user ID and friendships; no new duplicate row.
4. Restart app and confirm session restoration. Logout and restart must stay signed out.
5. Cancel browser flow, retry offline, then online. Test return after Android kills the app process.
6. Confirm email OTP still signs in. Delivery depends on configured SMTP.

This release uses browser OAuth for the existing hybrid app. It does not claim native Credential Manager login or completed physical-device verification.
