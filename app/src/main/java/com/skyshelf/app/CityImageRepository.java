package com.skyshelf.app;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class CityImageRepository {
    public interface ImageCallback {
        void onSuccess(String imageUrl);
        void onError(String errorMessage);
    }

    private static final OkHttpClient client = new OkHttpClient();

    private CityImageRepository() {
    }

    public static void fetchCityImage(WeatherRepository.LocationResult location, ImageCallback callback) {
        List<String> candidates = new ArrayList<>();
        if (location != null) {
            addCandidate(candidates, location.name);
            if (location.admin1 != null && !location.admin1.isEmpty()) {
                addCandidate(candidates, location.name + ", " + location.admin1);
            }
            if (location.country != null && !location.country.isEmpty()) {
                addCandidate(candidates, location.name + ", " + location.country);
            }
        }
        if (candidates.isEmpty()) {
            callback.onError("No city name available for image lookup.");
            return;
        }
        fetchFirstAvailableSummaryImage(candidates, 0, callback);
    }

    private static void addCandidate(List<String> candidates, String candidate) {
        if (candidate == null) {
            return;
        }
        String trimmed = candidate.trim();
        if (!trimmed.isEmpty() && !candidates.contains(trimmed)) {
            candidates.add(trimmed);
        }
    }

    private static void fetchFirstAvailableSummaryImage(List<String> candidates, int index, ImageCallback callback) {
        if (index >= candidates.size()) {
            callback.onError("No city image was found.");
            return;
        }

        String pageTitle = candidates.get(index);
        String encodedTitle;
        try {
            encodedTitle = URLEncoder.encode(pageTitle, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            callback.onError("Could not encode city image lookup.");
            return;
        }

        Request request = new Request.Builder()
                .url("https://en.wikipedia.org/api/rest_v1/page/summary/" + encodedTitle)
                .header("User-Agent", "SkyShelf/1.0 (Android)")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                fetchFirstAvailableSummaryImage(candidates, index + 1, callback);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() == null ? "" : response.body().string();
                response.close();

                if (!response.isSuccessful()) {
                    fetchFirstAvailableSummaryImage(candidates, index + 1, callback);
                    return;
                }

                try {
                    JSONObject json = new JSONObject(responseBody);
                    String type = json.optString("type", "");
                    if (!"disambiguation".equalsIgnoreCase(type)) {
                        // Prefer originalimage for detail-page hero photos. It is usually much
                        // higher resolution than thumbnail, so centerCrop has enough pixels
                        // for tall phone screens. Fall back to thumbnail when Wikipedia only
                        // provides a smaller image.
                        JSONObject originalImage = json.optJSONObject("originalimage");
                        String originalSource = originalImage == null ? "" : originalImage.optString("source", "");
                        if (!originalSource.isEmpty()) {
                            callback.onSuccess(originalSource);
                            return;
                        }

                        JSONObject thumbnail = json.optJSONObject("thumbnail");
                        String thumbnailSource = thumbnail == null ? "" : thumbnail.optString("source", "");
                        if (!thumbnailSource.isEmpty()) {
                            callback.onSuccess(thumbnailSource);
                            return;
                        }
                    }
                    fetchFirstAvailableSummaryImage(candidates, index + 1, callback);
                } catch (JSONException e) {
                    fetchFirstAvailableSummaryImage(candidates, index + 1, callback);
                }
            }
        });
    }
}
