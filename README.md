# Buzz Me In

Android app that auto-answers calls from a building buzzer (enterphone) and presses the
door-open key (DTMF), so deliveries get let in while you sleep. All other calls ring normally.

## How it works
Android only lets the **default Phone app** answer calls and send keypad tones, so this app
replaces the dialer (incoming/in-call screen, dial pad, recents). When a call arrives from a
configured number, inside its active time window, while no other call is in progress, it:
silences the ringer → answers → mutes the mic → waits → presses the keys → hangs up, and
logs + notifies. If answering fails within 10 s it falls back to normal ringing.

## Build
    ./gradlew assembleRelease   # needs Android SDK (local.properties sdk.dir), JDK 17
Output: `app/build/outputs/apk/release/app-release.apk`.

To sign with your own key, create `keystore.properties` in the project root (gitignored):

    storeFile=app/your.keystore
    storePassword=...
    keyAlias=...
    keyPassword=...

Without it the release APK is signed with the local debug key.

## Install on a phone
Sideloaded apps on Android 15+ are blocked from becoming the Phone app ("App was denied
access"). Fix: Settings → Apps → Buzz Me In → ⋮ → Allow restricted settings, then retry.

## Undo
Settings → Apps → Default apps → Phone app → pick your original Phone app.
