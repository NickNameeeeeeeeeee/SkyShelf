package com.skyshelf.app;

import android.app.Application;
import android.content.SharedPreferences;

/**
 * Performs lightweight app-wide initialization before any activity reads persisted data.
 */
public final class SkyShelfApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        AppPrefs.removeIncompatibleValues(preferences);
    }
}
