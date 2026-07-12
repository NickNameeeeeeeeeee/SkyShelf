package com.skyshelf.app;

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

    static boolean isPerUserCityKey(String key) {
        return key != null && key.endsWith(CITY_INFO_SUFFIX);
    }
}
