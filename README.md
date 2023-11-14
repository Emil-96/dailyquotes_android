# DailyQuotes frontend for Android

This is the Android frontend for the DailyQuotes app.

## Building your own app

I recommend copying the repo and opening it with [Android Studio](https://developer.android.com/studio).

If that is not possible you can use the command line to build the app. For that, copy the repo and open the root directory in the terminal.
Run the following command.

On Windows:

	gradlew.bat assembleDebug

On Linux:

	./gradlew assembleDebug

The .apk file will be found under `app/build/outputs/apk/app-debug.apk` and can be installed on any Android device.

## Documentation

The code should be sufficiently documented in comments in the actual code.

***Note:***  
Composable functions in [Jetpack Compose](https://developer.android.com/jetpack/compose) draw their content directly when called and thus have no return value. For simplicity sake I have documented the content of those functions as the functions *returning* it.

## Current State

The app is currently under development and is not ready to be used.

### Known issues

- The profile screen always shows a "not logged in" message
- When logging in, it loads indefinitely (leaving the screen manually works)
- When opening the app the first time and subsequent times, loading a quote not always happens (sometimes you have to open the profile screen and go back in order for it to load properly)

### Things coming in the future

- The possibility to save and view quotes