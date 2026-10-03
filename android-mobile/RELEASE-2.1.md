# Xparty Android 2.1.0 — interface preview

A visible Android-only redesign: dedicated welcome entry (Sign in, Create account, Guest), redesigned home, portrait call tiles of equal size, compact top navigation, app-menu sheet, separate volume sheet, secondary playback controls, a new Browse heading and floating bottom navigation. Keeps the original logo, Made4Love dedication, room backend and playback synchronization logic.

Accounts now distinguish registration from sign-in with `shouldCreateUser`, validate contact details, bind verification to the requested address and disable duplicate submissions. Existing profile, account deletion and friends code remains.

## Activation blocker

The live `/api/accounts/config` returns `enabled:false` and `phoneEnabled:false`. This APK does NOT activate real accounts by itself. The owner's Supabase project must have `xparty-service/accounts/schema.sql` applied, email OTP configured, and optionally a phone SMS provider enabled. The room service needs SUPABASE_URL and SUPABASE_ANON_KEY (publishable/anon key only), plus SUPABASE_PHONE_ENABLED=true only after SMS configuration. No service-role secret belongs in client code. Provider setup and live email/SMS delivery are not verified. Google embedded-browser OAuth also requires a separate Android browser/deep-link integration review before production.

## Verification

- Palette and ambient lifecycle tests passed.
- Two browser clients passed simulated YouTube play/approval, synthetic incoming WebRTC audio, repeat Call/Chat swipes, search timeout and responsive width checks.
- Welcome/create-account/sign-in/guest navigation checked in those clients.
- Separate mock auth-provider test covers sign-in versus registration, address binding and phone validation.
- Actual Android hardware, network-to-network calls, real OTP delivery and Play Store readiness remain unverified.

This is an installable interface preview, not a claim that every previously requested feature or account integration is complete. Existing 2.0 and 2.0.1 APKs remain rollback references. Do not change the website deployment for this Android-only release.
