package com.skyshelf.app;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class WeatherRepository {

    public interface WeatherCallback {
        void onSuccess(WeatherSnapshot weatherSnapshot);
        void onError(String errorMessage);
    }

    public interface LocationCallback {
        void onSuccess(LocationResult locationResult);
        void onError(String errorMessage);
    }

    public interface LocationListCallback {
        void onSuccess(ArrayList<LocationResult> locationResults);
        void onError(String errorMessage);
    }

    public static final class LocationResult {
        public final String name;
        public final String country;
        public final String countryCode;
        public final String admin1;
        public final double latitude;
        public final double longitude;
        public final String timezone;

        public LocationResult(String name, String country, String countryCode, String admin1, double latitude, double longitude, String timezone) {
            this.name = name;
            this.country = country;
            this.countryCode = countryCode;
            this.admin1 = admin1;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timezone = timezone;
        }

        public String getDisplayName() {
            StringBuilder builder = new StringBuilder(name == null || name.isEmpty() ? "Unknown city" : name);
            if (admin1 != null && !admin1.isEmpty()) {
                builder.append(", ").append(admin1);
            }
            if (countryCode != null && !countryCode.isEmpty()) {
                builder.append(", ").append(countryCode);
            } else if (country != null && !country.isEmpty()) {
                builder.append(", ").append(country);
            }
            return builder.toString();
        }
    }

    public static final class WeatherSnapshot {
        public final LocationResult location;
        public final double temperatureFahrenheit;
        public final double humidityPercent;
        public final String description;
        public final double windSpeedMph;
        public final double windDirectionDegrees;
        public final String observationTime;

        public WeatherSnapshot(LocationResult location,
                               double temperatureFahrenheit,
                               double humidityPercent,
                               String description,
                               double windSpeedMph,
                               double windDirectionDegrees,
                               String observationTime) {
            this.location = location;
            this.temperatureFahrenheit = temperatureFahrenheit;
            this.humidityPercent = humidityPercent;
            this.description = description;
            this.windSpeedMph = windSpeedMph;
            this.windDirectionDegrees = windDirectionDegrees;
            this.observationTime = observationTime;
        }
    }

    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto";
    private static final OkHttpClient client = new OkHttpClient();

    private WeatherRepository() {
        // Utility class
    }

    public static void fetchCurrentWeather(String cityName, WeatherCallback callback) {
        lookupCity(cityName, new LocationCallback() {
            @Override
            public void onSuccess(LocationResult locationResult) {
                fetchForecastForLocation(locationResult, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public static void lookupCity(String cityName, LocationCallback callback) {
        searchCities(cityName, 1, new LocationListCallback() {
            @Override
            public void onSuccess(ArrayList<LocationResult> locationResults) {
                if (locationResults.isEmpty()) {
                    callback.onError("No matching city was found.");
                } else {
                    callback.onSuccess(locationResults.get(0));
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public static void searchCities(String cityName, int count, LocationListCallback callback) {
        String trimmedCityName = cityName == null ? "" : cityName.trim();
        if (trimmedCityName.isEmpty()) {
            callback.onError("City name is missing.");
            return;
        }

        String encodedCity;
        try {
            encodedCity = URLEncoder.encode(trimmedCityName, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            callback.onError("Could not encode city name: " + e.getMessage());
            return;
        }

        int safeCount = Math.max(1, Math.min(count, 12));
        String url = String.format(Locale.US,
                "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=%d&language=en&format=json",
                encodedCity, safeCount);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Could not reach the geocoding service: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() == null ? "" : response.body().string();
                response.close();

                if (!response.isSuccessful()) {
                    callback.onError("Geocoding service returned HTTP " + response.code() + ".");
                    return;
                }

                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray results = jsonObject.optJSONArray("results");
                    if (results == null || results.length() == 0) {
                        callback.onError("No matching city was found.");
                        return;
                    }

                    ArrayList<LocationResult> parsedResults = new ArrayList<>();
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        parsedResults.add(new LocationResult(
                                result.optString("name", trimmedCityName),
                                result.optString("country", ""),
                                result.optString("country_code", ""),
                                result.optString("admin1", ""),
                                result.getDouble("latitude"),
                                result.getDouble("longitude"),
                                result.optString("timezone", "auto")
                        ));
                    }
                    callback.onSuccess(parsedResults);
                } catch (JSONException e) {
                    callback.onError("Could not read the city result: " + e.getMessage());
                }
            }
        });
    }

    private static void fetchForecastForLocation(LocationResult locationResult, WeatherCallback callback) {
        String url = String.format(Locale.US, FORECAST_URL, locationResult.latitude, locationResult.longitude);
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Could not reach the weather service: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() == null ? "" : response.body().string();
                response.close();

                if (!response.isSuccessful()) {
                    callback.onError("Weather service returned HTTP " + response.code() + ".");
                    return;
                }

                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONObject current = jsonObject.getJSONObject("current");
                    int weatherCode = current.optInt("weather_code", -1);
                    WeatherSnapshot weatherSnapshot = new WeatherSnapshot(
                            locationResult,
                            current.optDouble("temperature_2m", Double.NaN),
                            current.optDouble("relative_humidity_2m", Double.NaN),
                            weatherCodeToDescription(weatherCode),
                            current.optDouble("wind_speed_10m", Double.NaN),
                            current.optDouble("wind_direction_10m", Double.NaN),
                            current.optString("time", "")
                    );
                    callback.onSuccess(weatherSnapshot);
                } catch (JSONException e) {
                    callback.onError("Could not read the weather result: " + e.getMessage());
                }
            }
        });
    }

    public static String getWindDirection(double degrees) {
        if (Double.isNaN(degrees)) {
            return "";
        }
        if (degrees >= 0 && degrees < 22.5 || degrees >= 337.5 && degrees < 360) {
            return "North";
        } else if (degrees >= 22.5 && degrees < 67.5) {
            return "North-East";
        } else if (degrees >= 67.5 && degrees < 112.5) {
            return "East";
        } else if (degrees >= 112.5 && degrees < 157.5) {
            return "South-East";
        } else if (degrees >= 157.5 && degrees < 202.5) {
            return "South";
        } else if (degrees >= 202.5 && degrees < 247.5) {
            return "South-West";
        } else if (degrees >= 247.5 && degrees < 292.5) {
            return "West";
        } else {
            return "North-West";
        }
    }

    private static String weatherCodeToDescription(int code) {
        switch (code) {
            case 0:
                return "Clear sky";
            case 1:
            case 2:
            case 3:
                return "Partly cloudy";
            case 45:
            case 48:
                return "Fog";
            case 51:
            case 53:
            case 55:
                return "Drizzle";
            case 56:
            case 57:
                return "Freezing drizzle";
            case 61:
            case 63:
            case 65:
                return "Rain";
            case 66:
            case 67:
                return "Freezing rain";
            case 71:
            case 73:
            case 75:
            case 77:
                return "Snow";
            case 80:
            case 81:
            case 82:
                return "Rain showers";
            case 85:
            case 86:
                return "Snow showers";
            case 95:
            case 96:
            case 99:
                return "Thunderstorm";
            default:
                return "Current conditions";
        }
    }
}
