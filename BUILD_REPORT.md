# Build and validation report

## Passed

- Gradle 8.9 wrapper generation, with the published distribution SHA-256 recorded.
- `./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease` completed successfully against Android SDK 34 using JDK 21 and JVM target 17.
- **10 unit tests passed**, with no failures or skipped tests.
- Android lint: **0 errors, 5 warnings**. The remaining warnings report newer dependency versions; versions are pinned for the requested API 34 build.
- Debug APK signature verification passed using APK Signature Scheme v2.
- APK metadata confirms package `com.acousticshield.pro`, minimum SDK 26, target SDK 34, launcher activity, optional microphone hardware, and no Internet permission.
- A release APK was generated with R8 code/resource shrinking. It is **unsigned** and must be signed using your own release key before distribution.
- Source scan found no unfinished Kotlin implementation markers.

## Tests

1. Exact empirical barrier constants.
2. Alarm second-layer diminishing returns and inverse-square distance loss.
3. Alarm 20 dB floor.
4. Uncovered 1 m alarm baseline.
5. Classroom coverage saturation, area/cost calculation, and 30 dB floor.
6. Sensory comfort boundary values at 55 and 70 dB.
7. NIOSH reference durations at 85, 88, and 100 dB.
8. Additive dose across different levels and the 80 dB integration threshold.
9. Energy-average Leq rather than arithmetic averaging of decibels.
10. Rejection of invalid distance input.

## Runtime verification limitation

An API 34 x86_64 emulator was attempted using software acceleration because this execution environment has no KVM device. The APK installation succeeded and the package was listed, but Android did not reach a stable usable startup: activity/package services timed out or became unavailable. No app navigation or microphone flow is marked as runtime-tested. The source and APK should be exercised on a hardware-accelerated emulator and a physical Android phone before distribution.

Recommended device checks: microphone grant/deny and settings recovery; manual fallback; all five tabs; layered alarm calculations and preset persistence; budget sliders; sent meter readings; calibration persistence; pause/resume/save sessions; CSV sharing; background capture stop; font scaling; and microphone/reference-meter comparison.

The acoustic and NIOSH tools are educational estimates. See the README for the exact assumptions and limits. A successful build does not establish sound-level accuracy or certify the app for exposure monitoring.
