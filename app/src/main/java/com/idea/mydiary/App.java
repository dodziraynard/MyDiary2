package com.idea.mydiary;

import android.app.Application;

import com.bugfender.sdk.Bugfender;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Bugfender.init(this, "tmQaFNHHjZYm2DuYOEAfFFodXlIq2R0j", BuildConfig.DEBUG);
        Bugfender.enableCrashReporting();
        Bugfender.enableUIEventLogging(this);
        Bugfender.enableLogcatLogging();
    }
}
