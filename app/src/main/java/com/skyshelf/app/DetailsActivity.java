package com.skyshelf.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DetailsActivity extends AppCompatActivity {

    private String cityName;
    private String citySubtitle;
    private String imageUrl;
    private TextView title;
    private TextView subtitle;
    private TextView temperatureText;
    private TextView conditionText;
    private TextView updatedText;
    private TextView humidityText;
    private TextView windText;
    private TextView insightText;
    private WebView mapWebView;
    private ImageButton actionButton;
    private ScrollView detailScrollView;
    private View detailContentPanel;
    private int sheetExpandedScrollTarget;
    private int sheetSnapThreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        cityName = getIntent().getStringExtra("cityName");
        if (cityName == null || cityName.trim().isEmpty()) {
            cityName = getIntent().getStringExtra("city");
        }
        if (cityName == null || cityName.trim().isEmpty()) {
            cityName = getString(R.string.unknown_city);
        }
        citySubtitle = getIntent().getStringExtra("citySubtitle");
        if (citySubtitle == null || citySubtitle.trim().isEmpty()) {
            citySubtitle = getString(R.string.weather_collection);
        }
        imageUrl = getIntent().getStringExtra("imageUrl");

        ImageView cityImage = findViewById(R.id.detailCityImage);
        title = findViewById(R.id.detailTitle);
        subtitle = findViewById(R.id.detailSubtitle);
        temperatureText = findViewById(R.id.detailTemperatureText);
        conditionText = findViewById(R.id.detailConditionText);
        updatedText = findViewById(R.id.detailUpdatedText);
        humidityText = findViewById(R.id.detailHumidityText);
        windText = findViewById(R.id.detailWindText);
        insightText = findViewById(R.id.detailInsightText);
        mapWebView = findViewById(R.id.detailMapWebView);
        actionButton = findViewById(R.id.detailDeleteButton);
        detailScrollView = findViewById(R.id.detailScrollView);
        detailContentPanel = findViewById(R.id.detailContentPanel);

        title.setText(cityName);
        subtitle.setText(cleanSubtitle(citySubtitle));
        configureMapWebView();
        bindHeroImage(cityImage);
        loadCityWeatherAndInsight();
        refreshActionButton();
        configureDetailSnapSheet();

        findViewById(R.id.detailBackButton).setOnClickListener(v -> finish());
        actionButton.setOnClickListener(v -> toggleLibraryState());
    }

    @Override
    protected void onDestroy() {
        if (mapWebView != null) {
            mapWebView.destroy();
        }
        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureDetailSnapSheet() {
        detailScrollView.post(() -> {
            int screenHeight = detailScrollView.getHeight();
            if (screenHeight <= 0) {
                return;
            }

            int collapsedVisibleHeight = dp(160);
            int collapsedTopPadding = Math.max(dp(420), screenHeight - collapsedVisibleHeight);
            sheetExpandedScrollTarget = Math.max(0, collapsedTopPadding - dp(12));
            sheetSnapThreshold = Math.min(dp(190), Math.max(dp(96), sheetExpandedScrollTarget / 3));

            detailScrollView.setPadding(
                    detailScrollView.getPaddingLeft(),
                    collapsedTopPadding,
                    detailScrollView.getPaddingRight(),
                    detailScrollView.getPaddingBottom()
            );
            detailScrollView.setScrollY(0);
        });

        detailScrollView.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                detailScrollView.postDelayed(this::snapDetailSheet, 40);
            }
            return false;
        });
    }

    private void snapDetailSheet() {
        if (sheetExpandedScrollTarget <= 0) {
            return;
        }

        int currentScroll = detailScrollView.getScrollY();
        if (currentScroll < sheetSnapThreshold) {
            detailScrollView.smoothScrollTo(0, 0);
        } else if (currentScroll < sheetExpandedScrollTarget) {
            detailScrollView.smoothScrollTo(0, sheetExpandedScrollTarget);
        }
    }

    private void bindHeroImage(ImageView cityImage) {
        if (imageUrl != null && imageUrl.startsWith("http")) {
            ImageLoader.load(imageUrl, cityImage, R.drawable.ic_placeholder);
        } else {
            cityImage.setImageResource(R.drawable.ic_placeholder);
        }

        // Always try to refresh with the higher-resolution city image source.
        // This upgrades older saved thumbnail URLs without requiring users to re-add cities.
        WeatherRepository.lookupCity(cityName, new WeatherRepository.LocationCallback() {
            @Override
            public void onSuccess(WeatherRepository.LocationResult locationResult) {
                CityImageRepository.fetchCityImage(locationResult, new CityImageRepository.ImageCallback() {
                    @Override
                    public void onSuccess(String fetchedImageUrl) {
                        runOnUiThread(() -> ImageLoader.load(fetchedImageUrl, cityImage, R.drawable.ic_placeholder));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // Placeholder remains visible.
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Placeholder remains visible.
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureMapWebView() {
        mapWebView.setBackgroundColor(Color.TRANSPARENT);
        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
    }

    private void loadCityWeatherAndInsight() {
        conditionText.setText(R.string.detail_loading_weather);
        updatedText.setText(R.string.detail_fetching_live_conditions);
        insightText.setText(R.string.detail_loading_insight);
        loadMapFromQuery(cityName);

        WeatherRepository.fetchCurrentWeather(cityName, new WeatherRepository.WeatherCallback() {
            @Override
            public void onSuccess(WeatherRepository.WeatherSnapshot weatherSnapshot) {
                runOnUiThread(() -> {
                    title.setText(weatherSnapshot.location.name == null || weatherSnapshot.location.name.isEmpty()
                            ? cityName
                            : weatherSnapshot.location.name);
                    subtitle.setText(weatherSnapshot.location.getDisplayName());
                    bindWeather(weatherSnapshot);
                    loadMapForLocation(weatherSnapshot.location);
                    buildInsight(weatherSnapshot);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    conditionText.setText(R.string.detail_weather_unavailable);
                    updatedText.setText(errorMessage);
                    insightText.setText(R.string.detail_weather_unavailable_message);
                });
            }
        });
    }

    private void bindWeather(WeatherRepository.WeatherSnapshot snapshot) {
        temperatureText.setText(Double.isNaN(snapshot.temperatureFahrenheit)
                ? "--°"
                : Math.round(snapshot.temperatureFahrenheit) + "°");
        conditionText.setText(safeDescription(snapshot.description));
        updatedText.setText(snapshot.observationTime == null || snapshot.observationTime.isEmpty()
                ? getString(R.string.detail_live_conditions)
                : getString(R.string.detail_updated_format, snapshot.observationTime.replace('T', ' ')));
        humidityText.setText(getString(R.string.detail_humidity_format, formatWhole(snapshot.humidityPercent)));

        String direction = WeatherRepository.getWindDirection(snapshot.windDirectionDegrees);
        String windSpeed = formatWhole(snapshot.windSpeedMph);
        if (direction == null || direction.isEmpty()) {
            windText.setText(getString(R.string.detail_wind_format, windSpeed));
        } else {
            windText.setText(getString(R.string.detail_wind_direction_format, windSpeed, compactDirection(direction)));
        }
    }

    private void loadMapFromQuery(String query) {
        String encodedQuery = query == null ? "" : query.trim().replace(" ", "+");
        mapWebView.loadUrl("https://www.google.com/maps/search/?api=1&query=" + encodedQuery);
    }

    private void loadMapForLocation(WeatherRepository.LocationResult location) {
        double lat = location.latitude;
        double lon = location.longitude;
        String html = buildLeafletMapHtml(lat, lon, location.getDisplayName());
        mapWebView.loadDataWithBaseURL("https://www.openstreetmap.org/", html, "text/html", "UTF-8", null);
    }

    private String buildLeafletMapHtml(double latitude, double longitude, String label) {
        String safeLabel = label == null ? cityName : label.replace("'", "\\'");
        return "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0'>"
                + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
                + "<style>"
                + "html,body,#map{height:100%;width:100%;margin:0;padding:0;background:#eef2f5;overflow:hidden;}"
                + ".leaflet-control-zoom{margin:16px!important;border:0!important;border-radius:18px!important;overflow:hidden;"
                + "box-shadow:0 10px 30px rgba(0,0,0,.18)!important;background:rgba(255,255,255,.96)!important;}"
                + ".leaflet-control-zoom a{width:44px!important;height:44px!important;line-height:44px!important;font-size:26px!important;"
                + "font-weight:700!important;color:#111113!important;background:rgba(255,255,255,.96)!important;border:0!important;}"
                + ".leaflet-control-zoom-in{border-bottom:1px solid rgba(0,0,0,.12)!important;}"
                + ".leaflet-top.leaflet-right{top:0!important;right:0!important;}"
                + ".leaflet-control-attribution{display:none!important;}"
                + "</style></head><body><div id='map'></div>"
                + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "<script>"
                + "var map=L.map('map',{zoomControl:false,attributionControl:false}).setView(["
                + latitude + "," + longitude + "],13);"
                + "L.control.zoom({position:'topright'}).addTo(map);"
                + "L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',{subdomains:'abcd',maxZoom:20}).addTo(map);"
                + "L.circleMarker([" + latitude + "," + longitude + "],{radius:8,color:'#0a84ff',weight:3,fillColor:'#5ac8fa',fillOpacity:0.9}).addTo(map).bindPopup('" + safeLabel + "');"
                + "</script></body></html>";
    }

    private void buildInsight(WeatherRepository.WeatherSnapshot snapshot) {
        if (!NvidiaNimApiClient.isConfigured(this)) {
            insightText.setText(buildLocalInsight(snapshot));
            return;
        }

        String prompt = "Create one concise, practical weather insight for " + snapshot.location.getDisplayName() + ". " +
                "Current weather: " + safeDescription(snapshot.description) + ", temperature " + formatOne(snapshot.temperatureFahrenheit) +
                " °F, humidity " + formatOne(snapshot.humidityPercent) + "%, wind " + formatOne(snapshot.windSpeedMph) +
                " mph. Give 3-5 sentences. Focus on what the user should wear, carry, or consider doing today.";

        NvidiaNimApiClient.generateAnswer(this, prompt, new NvidiaNimApiClient.NimCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> insightText.setText(result == null || result.trim().isEmpty()
                        ? buildLocalInsight(snapshot)
                        : result.trim()));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> insightText.setText(buildLocalInsight(snapshot)));
            }
        });
    }

    private String buildLocalInsight(WeatherRepository.WeatherSnapshot snapshot) {
        StringBuilder insight = new StringBuilder();
        String description = safeDescription(snapshot.description).toLowerCase(Locale.US);
        insight.append("Current conditions are ").append(description)
                .append(" with a temperature around ").append(formatOne(snapshot.temperatureFahrenheit)).append(" °F. ");

        if (!Double.isNaN(snapshot.temperatureFahrenheit) && snapshot.temperatureFahrenheit >= 86) {
            insight.append("Dress lightly, drink water, and avoid staying in direct sun too long. ");
        } else if (!Double.isNaN(snapshot.temperatureFahrenheit) && snapshot.temperatureFahrenheit <= 45) {
            insight.append("Wear a warm outer layer, especially if you will be outside for a while. ");
        } else {
            insight.append("The temperature looks comfortable for normal daily plans. ");
        }

        if (description.contains("rain") || description.contains("drizzle") || description.contains("thunder")) {
            insight.append("Carry an umbrella or waterproof jacket. ");
        }
        if (!Double.isNaN(snapshot.humidityPercent) && snapshot.humidityPercent >= 70) {
            insight.append("Humidity may make it feel heavier, so breathable clothing helps. ");
        }
        if (!Double.isNaN(snapshot.windSpeedMph) && snapshot.windSpeedMph >= 18) {
            insight.append("Wind may affect walking, biking, and loose items. ");
        }
        insight.append("Check again before leaving if your plans depend on outdoor conditions.");
        return insight.toString();
    }

    private String cleanSubtitle(String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.weather_collection);
        }
        return value.replace("Weather • Map • Insights", getString(R.string.weather_collection)).trim();
    }

    private String safeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return getString(R.string.detail_current_conditions);
        }
        return description.trim();
    }

    private String formatWhole(double value) {
        if (Double.isNaN(value)) {
            return "--";
        }
        return String.valueOf(Math.round(value));
    }

    private String formatOne(double value) {
        if (Double.isNaN(value)) {
            return getString(R.string.detail_unavailable_value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String compactDirection(String direction) {
        return direction.replace("North", "N")
                .replace("South", "S")
                .replace("East", "E")
                .replace("West", "W")
                .replace("-", "");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshActionButton() {
        if (isCitySaved()) {
            actionButton.setImageResource(R.drawable.ic_trash_24);
            actionButton.setColorFilter(getColor(R.color.white));
            actionButton.setContentDescription(getString(R.string.cd_remove_city));
        } else {
            actionButton.setImageResource(R.drawable.ic_plus_24);
            actionButton.setColorFilter(getColor(R.color.white));
            actionButton.setContentDescription(getString(R.string.cd_add_city));
        }
    }

    private boolean isCitySaved() {
        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));
        if (!isLoggedIn || currentUser.trim().isEmpty()) {
            return false;
        }

        String key = AppPrefs.citiesKey(currentUser);
        Set<String> currentSet = preferences.getStringSet(key, new HashSet<>());
        for (String cityInfo : currentSet) {
            String savedName = cityInfo.split(AppPrefs.CITY_INFO_SEPARATOR, 3)[0];
            if (savedName.equalsIgnoreCase(cityName)) {
                return true;
            }
        }
        return false;
    }

    private void toggleLibraryState() {
        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));
        if (!isLoggedIn || currentUser.trim().isEmpty()) {
            Toast.makeText(this, R.string.toast_login_manage_library, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        String key = AppPrefs.citiesKey(currentUser);
        Set<String> currentSet = preferences.getStringSet(key, new HashSet<>());
        Set<String> updatedSet = new HashSet<>();
        boolean cityWasSaved = false;

        for (String cityInfo : currentSet) {
            String savedName = cityInfo.split(AppPrefs.CITY_INFO_SEPARATOR, 3)[0];
            if (savedName.equalsIgnoreCase(cityName)) {
                cityWasSaved = true;
            } else {
                updatedSet.add(cityInfo);
            }
        }

        if (cityWasSaved) {
            preferences.edit().putStringSet(key, updatedSet).apply();
            Toast.makeText(this, getString(R.string.toast_city_removed, cityName), Toast.LENGTH_SHORT).show();
        } else {
            String value = cityName + AppPrefs.CITY_INFO_SEPARATOR + citySubtitle + AppPrefs.CITY_INFO_SEPARATOR + (imageUrl == null ? "" : imageUrl);
            updatedSet.addAll(currentSet);
            updatedSet.add(value);
            preferences.edit().putStringSet(key, updatedSet).apply();
            Toast.makeText(this, getString(R.string.toast_city_added, cityName), Toast.LENGTH_SHORT).show();
        }

        setResult(RESULT_OK);
        refreshActionButton();
    }
}
