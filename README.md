# AcousticShield Pro

Native Kotlin + Jetpack Compose Android app targeting Android 14 (API 34), min API 26.

The repository includes the complete app source, tests, Android resources, build documentation, and a GitHub Actions workflow.

## Build on GitHub

Open the **Actions** tab and run **Build AcousticShield Pro**. The workflow builds the debug APK and publishes it as the downloadable artifact **AcousticShieldPro-debug-apk**.

## Build locally

Install JDK 17 and Gradle 8.9, then run:

```sh
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app includes a live microphone meter with manual fallback, alarm simulator, classroom acoustics planner, NIOSH exposure planner, calibration profiles, saved sessions, CSV export, daily dose tracking, and alarm presets.

This is an educational acoustic estimate, not a certified SPL meter, dBA dosimeter, alarm guarantee, or hearing-protection rating.
