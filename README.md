# dailyquotes frontend for Android

This is the Android frontend for the DailyQuotes app.


## Building your own app

I recommend copying the repo and opening it with [Android Studio](https://developer.android.com/studio).

If that is not possible you can use the command line to build the app. For that, copy the repo and open the root directory in the terminal.
Run the following command.

On Windows:

	gradlew.bat assembleDebug

On Linux:

	./gradlew assembleDebug

The .apk file will be found under `app/build/outputs/apk/app-debug.apk` and can be installed on any device.