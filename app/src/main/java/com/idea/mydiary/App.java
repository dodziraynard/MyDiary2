package com.idea.mydiary;

import static com.idea.mydiary.Utils.enableStrictModeAll;

import android.app.Application;

import com.bugfender.sdk.Bugfender;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Ensure too much work is not being done on the main thread.
        enableStrictModeAll();

        Bugfender.init(this, "tmQaFNHHjZYm2DuYOEAfFFodXlIq2R0j", BuildConfig.DEBUG);
        Bugfender.enableCrashReporting();
        Bugfender.enableUIEventLogging(this);
        Bugfender.enableLogcatLogging();
    }
}
