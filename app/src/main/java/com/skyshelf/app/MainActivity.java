package com.skyshelf.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int TAB_HOME = 0;
    private static final int TAB_LIBRARY = 1;

    private ActivityResultLauncher<Intent> addCityLauncher;
    private RecyclerView cityRecyclerView;
    private CityAdapter cityAdapter;
    private SharedPreferences userPrefs;
    private String currentUsername;
    private boolean isLoggedIn;
    private TextView emptyStateText;
    private TextView profileChipText;
    private ImageView profileChipImage;
    private View profileChipContainer;
    private TextView homeTitleText;
    private View homeHeaderContent;
    private DraggableSegmentedControl navSegmentedControl;
    private int currentTab = TAB_HOME;
    private boolean headerContentVisible = true;

    private ArrayList<City> suggestedCities = new ArrayList<>();
    private ArrayList<City> libraryCities = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_SkyShelf);
        super.onCreate(savedInstanceState);

        userPrefs = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        clearLegacyCityCollectionsIfNeeded();
        isLoggedIn = userPrefs.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        currentUsername = userPrefs.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt("currentTab", TAB_HOME);
        }

        setContentView(R.layout.activity_main);

        homeTitleText = findViewById(R.id.homeTitleText);
        homeHeaderContent = findViewById(R.id.homeHeaderContent);
        emptyStateText = findViewById(R.id.emptyStateText);
        profileChipText = findViewById(R.id.profileChipText);
        profileChipImage = findViewById(R.id.profileChipImage);
        profileChipContainer = findViewById(R.id.profileChipContainer);
        View buttonSearch = findViewById(R.id.buttonSearch);
        navSegmentedControl = findViewById(R.id.navSegmentedControl);
        navSegmentedControl.setLabels(getString(R.string.home), getString(R.string.library));
        navSegmentedControl.setSelectedIndex(currentTab, false);
        cityRecyclerView = findViewById(R.id.recyclerViewCities);

        updateProfileChip();
        suggestedCities = buildSuggestedCities();

        cityAdapter = new CityAdapter();
        cityAdapter.setOnDataChangeListener(cities -> {
            if (isLoggedIn && currentTab == TAB_LIBRARY) {
                libraryCities = new ArrayList<>(cities);
                saveCitiesToPrefs(libraryCities);
            }
        });

        cityRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        cityRecyclerView.setAdapter(cityAdapter);
        cityRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateFloatingHeaderVisibility(recyclerView.computeVerticalScrollOffset() <= 2);
            }
        });

        addCityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleAddCityResult
        );

        buttonSearch.setOnClickListener(view -> {
            if (!isLoggedIn) {
                Toast.makeText(this, R.string.toast_login_add_city, Toast.LENGTH_SHORT).show();
                openSettingsSheet();
                return;
            }
            Intent intent = new Intent(MainActivity.this, AddCity.class);
            intent.putExtra("sourceTab", currentTab);
            addCityLauncher.launch(intent);
        });

        navSegmentedControl.setOnSelectionChangedListener(this::switchToTab);

        View.OnClickListener openSettings = view -> openSettingsSheet();
        profileChipContainer.setOnClickListener(openSettings);
        profileChipText.setOnClickListener(openSettings);

        libraryCities = loadCitiesFromPrefs();
        refreshVisibleCities();
        updateTabVisuals();
    }

    private void openSettingsSheet() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        overridePendingTransition(0, 0);
    }


    private void updateProfileChip() {
        String initials = isLoggedIn ? initialsFor(currentUsername) : getString(R.string.profile_initial_guest);
        profileChipText.setText(initials);
        profileChipText.setContentDescription(isLoggedIn
                ? getString(R.string.profile_content_description_format, currentUsername)
                : getString(R.string.guest_profile_content_description));
        String uriString = isLoggedIn ? userPrefs.getString(AppPrefs.profilePhotoKey(currentUsername), "") : "";
        if (uriString != null && !uriString.trim().isEmpty()) {
            try {
                profileChipImage.setImageURI(Uri.parse(uriString));
                profileChipImage.setVisibility(View.VISIBLE);
                profileChipText.setVisibility(View.GONE);
                return;
            } catch (Exception ignored) {
                // Fall through to initials.
            }
        }
        profileChipImage.setImageDrawable(null);
        profileChipImage.setVisibility(View.GONE);
        profileChipText.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean latestLoginState = userPrefs.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String latestUsername = userPrefs.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));
        boolean identityChanged = latestLoginState != isLoggedIn || !latestUsername.equals(currentUsername);
        isLoggedIn = latestLoginState;
        currentUsername = latestUsername;
        updateProfileChip();
        if (identityChanged) {
            libraryCities = loadCitiesFromPrefs();
        } else if (isLoggedIn) {
            libraryCities = loadCitiesFromPrefs();
        } else {
            libraryCities = new ArrayList<>();
        }
        refreshVisibleCities();
        updateTabVisuals();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentTab", currentTab);
    }

    private void clearLegacyCityCollectionsIfNeeded() {
        int schemaVersion = userPrefs.getInt(AppPrefs.KEY_CITY_DATA_SCHEMA_VERSION, 0);
        if (schemaVersion >= 2) {
            return;
        }

        SharedPreferences.Editor editor = userPrefs.edit();
        for (String key : userPrefs.getAll().keySet()) {
            if (AppPrefs.isPerUserCityKey(key) || key.equals(AppPrefs.LEGACY_CITIES_KEY) || key.equals(AppPrefs.LEGACY_CITY_IMAGES_KEY)) {
                editor.remove(key);
            }
        }
        editor.putInt(AppPrefs.KEY_CITY_DATA_SCHEMA_VERSION, AppPrefs.CURRENT_CITY_DATA_SCHEMA_VERSION);
        editor.apply();
    }

    private void handleAddCityResult(ActivityResult result) {
        Intent data = result.getData();
        if (result.getResultCode() != RESULT_OK) {
            if (data != null && data.hasExtra("switchTab")) {
                int targetTab = data.getIntExtra("switchTab", currentTab);
                switchToTab(targetTab, targetTab > currentTab ? 1 : -1);
            }
            return;
        }
        if (data == null) {
            return;
        }

        String cityName = data.getStringExtra("cityName");
        String citySubtitle = data.getStringExtra("citySubtitle");
        String imageUrl = data.getStringExtra("imageUrl");

        if (cityName == null || cityName.trim().isEmpty()) {
            return;
        }
        cityName = cityName.trim();
        if (containsCity(libraryCities, cityName)) {
            Toast.makeText(this, getString(R.string.toast_city_already_saved, cityName), Toast.LENGTH_SHORT).show();
            return;
        }

        City newCity = new City(cityName, citySubtitle, imageUrl);
        libraryCities.add(newCity);
        saveCitiesToPrefs(libraryCities);
        if (currentTab == TAB_LIBRARY) {
            cityAdapter.setItems(new ArrayList<>(libraryCities));
        }
        Toast.makeText(this, getString(R.string.toast_city_added, cityName), Toast.LENGTH_SHORT).show();
    }

    private void switchToTab(int tab, int direction) {
        int targetTab = tab <= TAB_HOME ? TAB_HOME : TAB_LIBRARY;
        if (targetTab == currentTab) {
            updateTabVisuals();
            return;
        }

        int resolvedDirection = direction == 0 ? (targetTab > currentTab ? 1 : -1) : direction;
        currentTab = targetTab;
        updateTabVisuals();

        cityRecyclerView.animate()
                .alpha(0f)
                .translationX(-resolvedDirection * dp(72))
                .setDuration(120)
                .withEndAction(() -> {
                    refreshVisibleCities();
                    cityRecyclerView.setTranslationX(resolvedDirection * dp(72));
                    cityRecyclerView.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(190)
                            .start();
                    cityRecyclerView.post(() -> updateFloatingHeaderVisibility(cityRecyclerView.computeVerticalScrollOffset() <= 2));
                })
                .start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void refreshVisibleCities() {
        if (currentTab == TAB_HOME) {
            homeTitleText.setText(R.string.home);
            emptyStateText.setVisibility(View.GONE);
            cityRecyclerView.setVisibility(View.VISIBLE);
            cityAdapter.setItems(new ArrayList<>(suggestedCities));
            return;
        }

        homeTitleText.setText(R.string.library);
        if (!isLoggedIn) {
            cityAdapter.setItems(new ArrayList<>());
            cityRecyclerView.setVisibility(View.GONE);
            emptyStateText.setText(R.string.empty_library_locked);
            emptyStateText.setVisibility(View.VISIBLE);
            return;
        }

        libraryCities = loadCitiesFromPrefs();
        cityAdapter.setItems(new ArrayList<>(libraryCities));
        boolean isEmpty = libraryCities.isEmpty();
        emptyStateText.setText(R.string.empty_library_first_city);
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cityRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateTabVisuals() {
        if (navSegmentedControl != null) {
            navSegmentedControl.setSelectedIndex(currentTab, true);
        }
    }

    private ArrayList<City> loadCitiesFromPrefs() {
        if (!isLoggedIn || currentUsername.trim().isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> cityInfoSet = userPrefs.getStringSet(AppPrefs.citiesKey(currentUsername), new HashSet<>());
        ArrayList<City> loadedCities = new ArrayList<>();
        for (String cityInfo : cityInfoSet) {
            String[] parts = cityInfo.split(AppPrefs.CITY_INFO_SEPARATOR, 3);
            String cityName = parts.length > 0 ? parts[0] : getString(R.string.unknown_city);
            String subtitle = parts.length > 1 ? parts[1] : getString(R.string.weather_collection);
            String imageUrl = parts.length > 2 ? parts[2] : "";

            if (parts.length == 2 && parts[1].startsWith("http")) {
                subtitle = getString(R.string.weather_collection);
                imageUrl = parts[1];
            }
            loadedCities.add(new City(cityName, subtitle, imageUrl == null ? "" : imageUrl));
        }
        loadedCities.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return loadedCities;
    }

    private void saveCitiesToPrefs(ArrayList<City> cities) {
        if (!isLoggedIn || currentUsername.trim().isEmpty()) {
            return;
        }

        Set<String> cityInfoSet = new HashSet<>();
        for (City city : cities) {
            cityInfoSet.add(city.getName() + AppPrefs.CITY_INFO_SEPARATOR + city.getSubtitle() + AppPrefs.CITY_INFO_SEPARATOR + (city.getImageUrl() == null ? "" : city.getImageUrl()));
        }
        userPrefs.edit().putStringSet(AppPrefs.citiesKey(currentUsername), cityInfoSet).apply();
    }

    private boolean containsCity(ArrayList<City> cities, String cityName) {
        for (City city : cities) {
            if (city.getName().equalsIgnoreCase(cityName)) {
                return true;
            }
        }
        return false;
    }

    private String initialsFor(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.profile_initial_guest);
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
        }
        return name.trim().substring(0, Math.min(2, name.trim().length())).toUpperCase(Locale.US);
    }

    private void updateFloatingHeaderVisibility(boolean shouldShow) {
        if (homeHeaderContent == null || shouldShow == headerContentVisible) {
            return;
        }
        headerContentVisible = shouldShow;
        homeHeaderContent.animate()
                .alpha(shouldShow ? 1f : 0f)
                .translationY(shouldShow ? 0f : -14f)
                .setDuration(180)
                .start();
    }

    private ArrayList<City> buildSuggestedCities() {
        ArrayList<City> cities = new ArrayList<>();

        // Home suggestions intentionally use the same live city/image pipeline as
        // searched cities. Keeping imageUrl empty lets CityAdapter resolve the
        // thumbnail through Open-Meteo + Wikimedia instead of relying on stale
        // bundled or hand-picked test images.
        cities.add(new City("Chicago", "Illinois, US • " + getString(R.string.weather_collection), ""));
        cities.add(new City("Tokyo", "Tokyo, JP • " + getString(R.string.weather_collection), ""));
        cities.add(new City("Paris", "Île-de-France, FR • " + getString(R.string.weather_collection), ""));
        cities.add(new City("Sydney", "New South Wales, AU • " + getString(R.string.weather_collection), ""));
        cities.add(new City("Cape Town", "Western Cape, ZA • " + getString(R.string.weather_collection), ""));
        cities.add(new City("Seoul", "Seoul, KR • " + getString(R.string.weather_collection), ""));
        return cities;
    }
}
