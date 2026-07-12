package com.skyshelf.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class AddCity extends AppCompatActivity implements SearchResultAdapter.SearchResultListener {

    private static final int SEARCH_EXPLORE = 0;
    private static final int SEARCH_LIBRARY = 1;

    private EditText editTextCityName;
    private TextView statusText;
    private ProgressBar progressBar;
    private DraggableSegmentedControl searchModeControl;
    private TextView promptTitleText;
    private View centeredPrompt;
    private View searchRoot;
    private View searchBottomDock;
    private RecyclerView recyclerViewSearchResults;
    private SearchResultAdapter searchResultAdapter;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;
    private int searchMode = SEARCH_EXPLORE;
    private int searchToken = 0;
    private int lastKeyboardBottom = 0;
    private int lastSystemBarsBottom = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.city_add);

        searchRoot = findViewById(R.id.searchRoot);
        searchBottomDock = findViewById(R.id.searchBottomDock);
        editTextCityName = findViewById(R.id.editTextCityName);
        statusText = findViewById(R.id.statusText);
        promptTitleText = findViewById(R.id.promptTitleText);
        centeredPrompt = findViewById(R.id.searchCenteredPrompt);
        progressBar = findViewById(R.id.searchProgressBar);
        searchModeControl = findViewById(R.id.searchModeControl);
        searchModeControl.setLabels(getString(R.string.explore), getString(R.string.library));
        searchModeControl.setSelectedIndex(searchMode, false);
        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);
        ImageButton buttonCancel = findViewById(R.id.buttonCancel);
        ImageButton buttonClearText = findViewById(R.id.buttonClearText);

        searchResultAdapter = new SearchResultAdapter(this);
        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSearchResults.setAdapter(searchResultAdapter);

        configureKeyboardDockLift();
        syncTopTabs();

        editTextCityName.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                triggerImmediateSearch();
                return true;
            }
            return false;
        });
        editTextCityName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                scheduleSearch(editable == null ? "" : editable.toString());
            }
        });

        buttonCancel.setOnClickListener(view -> finish());
        buttonClearText.setOnClickListener(v -> {
            editTextCityName.setText("");
            clearResults();
            editTextCityName.requestFocus();
        });

        searchModeControl.setOnSelectionChangedListener(this::switchSearchMode);
    }

    private void configureKeyboardDockLift() {
        ViewCompat.setOnApplyWindowInsetsListener(searchRoot, (view, insets) -> {
            int keyboardBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            lastKeyboardBottom = keyboardBottom;
            lastSystemBarsBottom = systemBarsBottom;
            int bottomMargin = (keyboardBottom > 0 ? keyboardBottom : systemBarsBottom) + dp(keyboardBottom > 0 ? 10 : 22);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) searchBottomDock.getLayoutParams();
            if (params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin;
                searchBottomDock.setLayoutParams(params);
            }
            searchRoot.post(() -> positionCenteredPrompt(keyboardBottom, systemBarsBottom));
            return insets;
        });

        searchRoot.post(() -> positionCenteredPrompt(0, 0));
    }

    private void positionCenteredPrompt(int keyboardBottom, int systemBarsBottom) {
        if (searchRoot == null || centeredPrompt == null || searchModeControl == null || searchBottomDock == null) {
            return;
        }
        int rootHeight = searchRoot.getHeight();
        if (rootHeight <= 0) {
            return;
        }

        int topBoundary = searchModeControl.getBottom() + dp(34);
        int bottomBoundary = searchBottomDock.getTop() - dp(22);
        if (bottomBoundary <= topBoundary) {
            int insetBottom = keyboardBottom > 0 ? keyboardBottom : systemBarsBottom;
            bottomBoundary = rootHeight - insetBottom - dp(94);
        }
        if (bottomBoundary <= topBoundary) {
            bottomBoundary = rootHeight - dp(160);
        }

        int targetCenter = topBoundary + Math.max(0, bottomBoundary - topBoundary) / 2;
        float baseCenter = rootHeight / 2f;
        centeredPrompt.setTranslationY(targetCenter - baseCenter);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void syncTopTabs() {
        if (searchModeControl != null) {
            searchModeControl.setSelectedIndex(searchMode, true);
        }
        setPromptText(searchMode == SEARCH_EXPLORE ? getString(R.string.prompt_explore_title) : getString(R.string.prompt_library_title),
                searchMode == SEARCH_EXPLORE
                        ? getString(R.string.prompt_explore_subtitle)
                        : getString(R.string.prompt_library_subtitle));
    }

    private void switchSearchMode(int mode, int direction) {
        int targetMode = mode <= SEARCH_EXPLORE ? SEARCH_EXPLORE : SEARCH_LIBRARY;
        if (searchMode == targetMode) {
            syncTopTabs();
            return;
        }
        int resolvedDirection = direction == 0 ? (targetMode > searchMode ? 1 : -1) : direction;
        searchMode = targetMode;
        syncTopTabs();
        String currentQuery = editTextCityName.getText() == null ? "" : editTextCityName.getText().toString();

        animateSearchContentOut(resolvedDirection, () -> {
            scheduleSearch(currentQuery);
            animateSearchContentIn(resolvedDirection);
        });
    }


    private void animateSearchContentOut(int direction, Runnable endAction) {
        recyclerViewSearchResults.animate().cancel();
        centeredPrompt.animate().cancel();
        recyclerViewSearchResults.animate()
                .alpha(0f)
                .translationX(-direction * dp(64))
                .setDuration(100)
                .start();
        centeredPrompt.animate()
                .alpha(0f)
                .translationX(-direction * dp(64))
                .setDuration(100)
                .withEndAction(endAction)
                .start();
    }

    private void animateSearchContentIn(int direction) {
        recyclerViewSearchResults.setTranslationX(direction * dp(64));
        centeredPrompt.setTranslationX(direction * dp(64));
        recyclerViewSearchResults.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(170)
                .start();
        centeredPrompt.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(170)
                .start();
    }

    private void scheduleSearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }

        if (query.isEmpty()) {
            clearResults();
            return;
        }

        pendingSearchRunnable = () -> performSearch(query);
        searchHandler.postDelayed(pendingSearchRunnable, searchMode == SEARCH_LIBRARY ? 120 : 280);
    }

    private void triggerImmediateSearch() {
        if (pendingSearchRunnable != null) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
        }
        String query = editTextCityName.getText() == null ? "" : editTextCityName.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, R.string.toast_enter_city, Toast.LENGTH_SHORT).show();
            return;
        }
        performSearch(query);
    }

    private void performSearch(String query) {
        if (searchMode == SEARCH_LIBRARY) {
            searchLibrary(query);
        } else {
            searchExplore(query);
        }
    }

    private void clearResults() {
        searchToken++;
        progressBar.setVisibility(View.GONE);
        searchResultAdapter.setItems(new ArrayList<>());
        showCenteredPrompt(searchMode == SEARCH_EXPLORE
                        ? getString(R.string.prompt_explore_title)
                        : getString(R.string.prompt_library_title),
                searchMode == SEARCH_EXPLORE
                        ? getString(R.string.prompt_explore_subtitle)
                        : getString(R.string.prompt_library_subtitle));
    }

    private void showCenteredPrompt(String title, String subtitle) {
        setPromptText(title, subtitle);
        centeredPrompt.setVisibility(View.VISIBLE);
        searchRoot.post(() -> positionCenteredPrompt(lastKeyboardBottom, lastSystemBarsBottom));
    }

    private void hideCenteredPrompt() {
        centeredPrompt.setVisibility(View.GONE);
    }

    private void setPromptText(String title, String subtitle) {
        promptTitleText.setText(title);
        statusText.setText(subtitle);
    }

    private void searchExplore(String query) {
        final int token = ++searchToken;
        progressBar.setVisibility(View.VISIBLE);
        showCenteredPrompt(getString(R.string.searching_title), getString(R.string.searching_cities));

        WeatherRepository.searchCities(query, 8, new WeatherRepository.LocationListCallback() {
            @Override
            public void onSuccess(ArrayList<WeatherRepository.LocationResult> locationResults) {
                runOnUiThread(() -> {
                    if (token != searchToken) {
                        return;
                    }
                    progressBar.setVisibility(View.GONE);
                    ArrayList<City> cities = new ArrayList<>();
                    for (WeatherRepository.LocationResult locationResult : locationResults) {
                        cities.add(new City(locationResult.name, buildCitySubtitle(locationResult), ""));
                    }
                    searchResultAdapter.setItems(cities);
                    if (cities.isEmpty()) {
                        showCenteredPrompt(getString(R.string.prompt_no_results_title), getString(R.string.detail_no_matching_city));
                    } else {
                        hideCenteredPrompt();
                    }

                    for (int i = 0; i < locationResults.size(); i++) {
                        final int index = i;
                        WeatherRepository.LocationResult locationResult = locationResults.get(i);
                        CityImageRepository.fetchCityImage(locationResult, new CityImageRepository.ImageCallback() {
                            @Override
                            public void onSuccess(String imageUrl) {
                                runOnUiThread(() -> updateExploreItemImage(token, index, locationResult, imageUrl));
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // leave placeholder
                            }
                        });
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    if (token != searchToken) {
                        return;
                    }
                    progressBar.setVisibility(View.GONE);
                    searchResultAdapter.setItems(new ArrayList<>());
                    showCenteredPrompt(getString(R.string.prompt_no_results_title), errorMessage);
                });
            }
        });
    }

    private void updateExploreItemImage(int token, int index, WeatherRepository.LocationResult locationResult, String imageUrl) {
        if (token != searchToken) {
            return;
        }
        ArrayList<City> currentItems = searchResultAdapter.getItems();
        if (index < 0 || index >= currentItems.size()) {
            return;
        }
        currentItems.set(index, new City(locationResult.name, buildCitySubtitle(locationResult), imageUrl));
        searchResultAdapter.setItems(currentItems);
    }

    private void searchLibrary(String query) {
        ++searchToken;
        progressBar.setVisibility(View.GONE);
        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));
        if (!isLoggedIn || currentUser.trim().isEmpty()) {
            searchResultAdapter.setItems(new ArrayList<>());
            showCenteredPrompt(getString(R.string.prompt_library_locked_title), getString(R.string.prompt_library_locked_subtitle));
            return;
        }

        ArrayList<City> cities = loadLibraryCities(preferences, currentUser);
        ArrayList<City> matches = new ArrayList<>();
        String needle = query.trim().toLowerCase(Locale.US);
        for (City city : cities) {
            if (city.getName().toLowerCase(Locale.US).contains(needle)
                    || city.getSubtitle().toLowerCase(Locale.US).contains(needle)) {
                matches.add(city);
            }
        }
        searchResultAdapter.setItems(matches);
        if (matches.isEmpty()) {
            showCenteredPrompt(getString(R.string.prompt_no_results_title), getString(R.string.no_saved_city_matched_format, query));
        } else {
            hideCenteredPrompt();
        }
    }

    private ArrayList<City> loadLibraryCities(SharedPreferences preferences, String currentUser) {
        Set<String> cityInfoSet = preferences.getStringSet(AppPrefs.citiesKey(currentUser), new HashSet<>());
        ArrayList<City> loadedCities = new ArrayList<>();
        for (String cityInfo : cityInfoSet) {
            String[] parts = cityInfo.split(AppPrefs.CITY_INFO_SEPARATOR, 3);
            String cityName = parts.length > 0 ? parts[0] : getString(R.string.unknown_city);
            String subtitle = parts.length > 1 ? parts[1] : getString(R.string.weather_collection);
            String imageUrl = parts.length > 2 ? parts[2] : "";
            loadedCities.add(new City(cityName, subtitle, imageUrl));
        }
        loadedCities.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return loadedCities;
    }

    @Override
    public void onResultClicked(City city) {
        openDetails(city);
    }

    @Override
    public void onMoreClicked(View anchor, City city) {
        showActionsPopup(anchor, city);
    }

    private void showActionsPopup(View anchor, City city) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_search_actions, (ViewGroup) anchor.getRootView(), false);
        TextView actionPrimary = popupView.findViewById(R.id.actionPrimary);
        TextView actionSecondary = popupView.findViewById(R.id.actionSecondary);
        View divider = popupView.findViewById(R.id.actionDivider);

        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dp(10));

        if (searchMode == SEARCH_EXPLORE) {
            actionPrimary.setText(R.string.action_add_to_library);
            actionPrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_plus_24, 0, 0, 0);
            actionSecondary.setText(R.string.action_open_city);
            actionSecondary.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search_24, 0, 0, 0);
            divider.setVisibility(View.VISIBLE);

            actionPrimary.setOnClickListener(v -> {
                popupWindow.dismiss();
                addCityToLibrary(city);
            });
            actionSecondary.setOnClickListener(v -> {
                popupWindow.dismiss();
                openDetails(city);
            });
        } else {
            actionPrimary.setText(R.string.action_open_city);
            actionPrimary.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search_24, 0, 0, 0);
            actionSecondary.setText(R.string.action_remove_from_library);
            actionSecondary.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_trash_24, 0, 0, 0);
            divider.setVisibility(View.VISIBLE);

            actionPrimary.setOnClickListener(v -> {
                popupWindow.dismiss();
                openDetails(city);
            });
            actionSecondary.setOnClickListener(v -> {
                popupWindow.dismiss();
                removeCityFromLibrary(city);
            });
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        popupWindow.showAsDropDown(anchor, -dp(146), dp(4), Gravity.END);
    }

    private void addCityToLibrary(City city) {
        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));
        if (!isLoggedIn || currentUser.trim().isEmpty()) {
            Toast.makeText(this, R.string.toast_login_add_city, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        Set<String> cityInfoSet = new HashSet<>(preferences.getStringSet(AppPrefs.citiesKey(currentUser), new HashSet<>()));
        for (String cityInfo : cityInfoSet) {
            if (cityInfo.split(AppPrefs.CITY_INFO_SEPARATOR, 3)[0].equalsIgnoreCase(city.getName())) {
                Toast.makeText(this, getString(R.string.toast_city_already_saved, city.getName()), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        cityInfoSet.add(city.getName() + AppPrefs.CITY_INFO_SEPARATOR + city.getSubtitle() + AppPrefs.CITY_INFO_SEPARATOR + (city.getImageUrl() == null ? "" : city.getImageUrl()));
        preferences.edit().putStringSet(AppPrefs.citiesKey(currentUser), cityInfoSet).apply();
        Toast.makeText(this, getString(R.string.toast_city_added, city.getName()), Toast.LENGTH_SHORT).show();
    }

    private void removeCityFromLibrary(City city) {
        SharedPreferences preferences = getSharedPreferences(AppPrefs.PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(AppPrefs.KEY_IS_LOGGED_IN, false);
        String currentUser = preferences.getString(AppPrefs.KEY_CURRENT_USER, getString(R.string.guest));
        if (!isLoggedIn || currentUser.trim().isEmpty()) {
            Toast.makeText(this, R.string.toast_login_manage_library, Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> cityInfoSet = new HashSet<>(preferences.getStringSet(AppPrefs.citiesKey(currentUser), new HashSet<>()));
        Set<String> updatedSet = new HashSet<>();
        for (String cityInfo : cityInfoSet) {
            if (!cityInfo.split(AppPrefs.CITY_INFO_SEPARATOR, 3)[0].equalsIgnoreCase(city.getName())) {
                updatedSet.add(cityInfo);
            }
        }
        preferences.edit().putStringSet(AppPrefs.citiesKey(currentUser), updatedSet).apply();
        Toast.makeText(this, getString(R.string.toast_city_removed, city.getName()), Toast.LENGTH_SHORT).show();
        String currentQuery = editTextCityName.getText() == null ? "" : editTextCityName.getText().toString().trim();
        if (!currentQuery.isEmpty()) {
            searchLibrary(currentQuery);
        } else {
            clearResults();
        }
    }

    private void openDetails(City city) {
        Intent detailIntent = new Intent(this, DetailsActivity.class);
        detailIntent.putExtra("cityName", city.getName());
        detailIntent.putExtra("citySubtitle", city.getSubtitle());
        detailIntent.putExtra("imageUrl", city.getImageUrl() == null ? "" : city.getImageUrl());
        startActivity(detailIntent);
    }

    private String buildCitySubtitle(WeatherRepository.LocationResult location) {
        StringBuilder builder = new StringBuilder();
        if (location.admin1 != null && !location.admin1.isEmpty()) {
            builder.append(location.admin1);
        }
        if (location.countryCode != null && !location.countryCode.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(location.countryCode);
        } else if (location.country != null && !location.country.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(location.country);
        }
        return builder.isEmpty() ? getString(R.string.weather_collection) : builder + " • " + getString(R.string.weather_collection);
    }
}
