# DailyQuotes frontend for Android

This is the Android frontend for the DailyQuotes app.

This app was developed in parallel with a web version, which you can find [here](https://github.com/JanWilfert/DailyQuotesWebApp/).

## Building your own app

I recommend copying the repo and opening it with [Android Studio](https://developer.android.com/studio).

If that is not possible you can use the command line to build the app. For that, copy the repo and open the root directory in the terminal.
Run the following command.

On Linux:

	./gradlew assembleRelease

On Windows:

	gradlew.bat assembleRelease

The .apk file will be found under `app/build/outputs/apk/release/app-debug-unsigned.apk` and can be installed on any Android device.

## Documentation

The code should be sufficiently documented in comments in the actual code.

***Note:***  
Composable functions in [Jetpack Compose](https://developer.android.com/jetpack/compose) draw their content directly when called and thus have no return value. For simplicity sake I have documented the content of those functions as the functions *returning* it.

## Current State

The app has been finished in the context of the university course, but might contain issues.
If you encounter any issues, please [contact me](mailto:emil.ca.carls+github@gmail.com).