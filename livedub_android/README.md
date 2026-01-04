# LiveDub Android

## Setup
1. Download this folder.
2. Open **Android Studio**.
3. Select **Open an existing Project**.
4. Navigate to `livedub_android` and click OK.
5. Wait for Gradle Sync to finish.

## Requirements
- Android SDK 34
- JDK 17
- Android Device running Android 10 (API 29) or higher.

## Features Implemented
- **MediaProjection**: Captures internal audio from other apps (requires `ALLOW_CAPTURE_BY_SYSTEM` in target apps).
- **Foreground Service**: Keeps the app alive and processing audio.
- **Overlay**: Floating bubble to control the service.
- **Audio Pipeline**: 16kHz PCM capture setup for STT.

## Todo for Production
1. **STT Integration**: The `LiveDubService.kt` contains a placeholder loop `processAudioForSTT`. Integrate OpenAI Whisper (via `whisper.cpp` JNI) or Google Cloud Speech API here.
2. **Translation API**: Integrate DeepL or Google Translate API.
3. **Icons**: Replace default icons in `res/drawable`.

## CI/CD
A `codemagic.yaml` file is included for automated builds.
