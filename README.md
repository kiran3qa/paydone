# PayAlert Flutter App

This project is an Android-only Flutter application demonstrating a simple UI for toggling a listening feature, persisting state using `shared_preferences`, and communicating with native Android code via a `MethodChannel` (`pay_speaker/native`).

## Features

- Shows current state: **Listening: ON** / **OFF**
- Buttons to **TURN ON**, **TURN OFF**, and **OPEN NOTIFICATION ACCESS**
- Optional **TEST SPEAK** button for triggering a native method
- Stores `listeningEnabled` in shared preferences
- Calls native methods: `setListeningEnabled`, `getListeningEnabled`, `openNotificationAccessSettings`, `isNotificationListenerEnabled`, `testSpeak`

## Setup & Run

1. Ensure Flutter SDK is installed and available in PATH.
2. Open the project folder (`e:\payalert`) in VS Code or your preferred IDE.
3. Run `flutter pub get` to fetch dependencies.
4. Launch an Android emulator or connect an Android device.
5. Execute `flutter run` to build and install the app.

The app is currently configured for Android only and uses a minimal native implementation in `MainActivity.kt`.

## Notes

- Notification listener permission is declared in `AndroidManifest.xml`.
- The native side handles preferences and notification access checks. A helper `PayTts.speak()` class is included for text-to-speech.

---
