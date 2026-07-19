package com.skyshelf.app;

import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

final class AppPrefs {
    static final String PREFS_NAME = "UserPrefs";
    static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    static final String KEY_CURRENT_USER = "currentUser";
    static final String KEY_CITY_DATA_SCHEMA_VERSION = "city_data_schema_version";
    static final int CURRENT_CITY_DATA_SCHEMA_VERSION = 2;

    static final String CITY_INFO_SEPARATOR = "##";
    static final String LEGACY_CITIES_KEY = "cities";
    static final String LEGACY_CITY_IMAGES_KEY = "city_images";

    private static final String CITY_INFO_SUFFIX = "_cities_info";
    private static final String PASSWORD_SUFFIX = "_password";
    private static final String PROFILE_PHOTO_SUFFIX = "_profile_photo_uri";
    private static final String THEME_SUFFIX = "_theme";

    private AppPrefs() {
        // Utility class.
    }

    static String citiesKey(String username) {
        return username + CITY_INFO_SUFFIX;
    }

    static String passwordKey(String username) {
        return username + PASSWORD_SUFFIX;
    }

    static String profilePhotoKey(String username) {
        return username + PROFILE_PHOTO_SUFFIX;
    }

    static String themeKey(String username) {
        return username + THEME_SUFFIX;
    }


    /**
     * Removes only persisted values whose runtime type no longer matches the current schema.
     * This prevents restored data from an older build from crashing typed SharedPreferences reads.
     */
    static void removeIncompatibleValues(SharedPreferences preferences) {
        Map<String, ?> values = preferences.getAll();
        if (values.isEmpty()) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        boolean changed = false;

        changed |= removeUnlessType(editor, values, KEY_IS_LOGGED_IN, Boolean.class);
        changed |= removeUnlessType(editor, values, KEY_CURRENT_USER, String.class);
        changed |= removeUnlessType(editor, values, KEY_CITY_DATA_SCHEMA_VERSION, Integer.class);

        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isPerUserCityKey(key)) {
                if (!isStringSet(value)) {
                    editor.remove(key);
                    changed = true;
                }
            } else if (isPerUserStringKey(key) && !(value instanceof String)) {
                editor.remove(key);
                changed = true;
            }
        }

        if (changed) {
            // Commit synchronously because activities read these values immediately afterward.
            editor.commit();
        }
    }

    private static boolean removeUnlessType(SharedPreferences.Editor editor,
                                            Map<String, ?> values,
                                            String key,
                                            Class<?> expectedType) {
        Object value = values.get(key);
        if (value == null || expectedType.isInstance(value)) {
            return false;
        }
        editor.remove(key);
        return true;
    }

    private static boolean isStringSet(Object value) {
        if (!(value instanceof Set<?>)) {
            return false;
        }
        for (Object item : (Set<?>) value) {
            if (!(item instanceof String)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPerUserStringKey(String key) {
        if (key == null) {
            return false;
        }
        return key.endsWith(PASSWORD_SUFFIX)
                || key.endsWith(PROFILE_PHOTO_SUFFIX)
                || key.endsWith(THEME_SUFFIX)
                || key.equals("nvidia_nim_api_key")
                || key.equals("nvidia_nim_model")
                || key.equals("nvidia_nim_base_url");
    }

    static boolean isPerUserCityKey(String key) {
        return key != null && key.endsWith(CITY_INFO_SUFFIX);
    }
}
