# Luma Assistant

A clean, from-scratch Android voice assistant: chat with an AI by voice or
text, set reminders, and take notes. Built specifically to have **none** of
the hidden behavior found in the "Max Assistant" APK you looked at earlier.

## What it does (and only this)
- **Voice chat** — tap the mic, ask something, it answers out loud and on
  screen, using an AI API key you provide yourself in Settings.
- **Reminders** — say "remind me to call mom in 10 minutes" and it schedules
  a one-off local notification.
- **Notes** — say "note that the wifi password is..." and it's saved locally
  on-device (Room database), never uploaded anywhere.

## What it deliberately does *not* do
- No `READ_PHONE_STATE`, `CALL_PHONE`, `READ_CALL_LOG`, `CAMERA`,
  `READ_CONTACTS`, or Accessibility Service permissions — because a voice
  assistant doesn't need any of them.
- No `BOOT_COMPLETED` receiver — nothing launches itself when the phone
  restarts.
- No background service keeps microphone access open; `SpeechRecognizer`
  only runs while you're actively holding the mic button's session open.
- No remote "activation key" server, no auto-update mechanism, no bundled
  third-party control channel.

## Before you build
1. Open this folder in Android Studio (Koala or newer). It will fetch the
   Gradle wrapper automatically on first sync — this project's `build.gradle`
   files are provided, but the wrapper jar itself isn't checked in here.
2. You'll need your own AI API key (e.g. from console.anthropic.com) —
   `AiClient.kt` calls the Anthropic Messages API by default. You can point
   it at any other provider's REST endpoint if you prefer.
3. Run on a device or emulator with Google Play services (for
   `SpeechRecognizer` and `TextToSpeech` to work well).

## Where things live
- `ai/` — the API client and encrypted key storage
- `voice/` — speech-to-text and text-to-speech wrappers
- `features/` — reminders, notes, and the plain-text command parser
- `ui/` — Compose screens (Chat, Notes, Settings) and the color/theme tokens
- `data/` — Room database for notes

## Extending it safely
If you add a feature, ask: "does this need a permission a voice assistant
wouldn't normally need?" If yes, that's worth pausing on. Keep every action
traceable to something the user explicitly asked for in this session — that
one rule is what separates this app from the one you uninstalled.

## Automation features (added in v2)

These map to the "Max Assistant" feature list, built the transparent way:

| Feature | How it's built here | Status |
|---|---|---|
| Flashlight | `CameraManager` torch API | Working |
| Volume control | `AudioManager` | Working |
| Wi-Fi / Bluetooth toggle | Opens the system's own quick panel (Android 10+ blocks silent toggling by design — this is intentional, not a limitation of the app) | Working, requires your tap |
| Open app / type / tap / scroll by voice | Accessibility Service, off by default, enabled from Settings → "Open Accessibility Settings" | Working |
| Take a selfie | `camera2` headless capture, on explicit "take a selfie" command | Working |
| Anti-Theft Guard | Device Admin scoped to **lock + watch-login only** (no wipe, no password reset). After 3 failed unlocks, takes one local photo and notifies. Toggle in Settings, revocable anytime | Working, but see limitation below |
| Screen/scene analysis via AI vision | Not yet built. Needs `MediaProjection`, which Android requires the **user to approve via a system dialog every single time** it's used — there's no way to make this silent, by design | Stub — responds explaining this |
| Incoming call name announcer / voice answer-decline | Not yet built. Real risk: Play Store restricts call-answering to the device's **default Phone app** as of recent policy; a side-loaded personal app can still do it via Accessibility clicking the dialer's on-screen Answer/Decline buttons (best-effort, OEM-dependent, won't work on every phone) | Not built — flagged as a real platform constraint, not just missing code |
| WhatsApp message automation | Same Accessibility "type into focused field" + "tap send" primitives could do this | **Not implemented on purpose.** This specifically violates WhatsApp's Terms of Service and risks your WhatsApp account being banned by Meta's automation detection — this is true no matter who builds it or how well-intentioned. If you still want it, the primitives (`typeIntoFocusedField`, `tapByText`) already support it; wire it up knowing that risk is real and yours to accept. |

### Important limitation: Anti-Theft Guard's local-only photo
A real "find my stolen phone" feature needs the intruder photo to reach the
*owner*, not stay on the phone the thief now has. This version saves and
notifies locally only, which is enough to test the flow but not to actually
protect you if the phone is stolen. Making that real requires a backend to
receive an uploaded photo — genuinely useful, but a separate, bigger
project, and worth being clear-eyed that "photo leaves your device" is
exactly the property that made the original app's camera behavior
concerning; the difference here is that *you* would configure where it goes
and can see the code doing it, rather than trusting an unknown vendor.

### The one-line test for anything you add next
Before wiring in a new automation: does this action happen because the user
said/typed something in this session, or could it happen without them
noticing? Keep everything on the first side of that line.
