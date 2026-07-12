package com.skyshelf.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class NvidiaNimApiClient {

    public interface NimCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public static final String PREF_NIM_API_KEY = "nvidia_nim_api_key";
    public static final String PREF_NIM_MODEL = "nvidia_nim_model";
    public static final String PREF_NIM_BASE_URL = "nvidia_nim_base_url";

    // NVIDIA NIM configuration. This placeholder key is intentionally kept as-is;
    // replace it later with the real NIM key in local.properties or here if needed.
    public static final String DEFAULT_BASE_URL = "https://integrate.api.nvidia.com/v1";
    public static final String DEFAULT_API_KEY = "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";
    public static final String DEFAULT_MODEL = "mistralai/mistral-large-3-675b-instruct-2512";

    private static final double DESC_TEMPERATURE = 0.2;
    private static final int DESC_MAX_TOKENS = 160;
    private static final int HTTP_TIMEOUT_SEC = 90;
    private static final int MAX_RETRIES = 6;
    private static final long RETRY_BASE_SLEEP_MS = 1000L;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
            .callTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
            .build();
    private static final Handler retryHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private NvidiaNimApiClient() {
        // Utility class.
    }

    public static boolean isConfigured(Context context) {
        return !getApiKey(context).isEmpty();
    }

    public static String getApiKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        String savedKey = prefs.getString(PREF_NIM_API_KEY, "");
        if (savedKey != null && !savedKey.trim().isEmpty()) {
            return savedKey.trim();
        }
        String buildConfigKey = BuildConfig.NVIDIA_NIM_API_KEY == null ? "" : BuildConfig.NVIDIA_NIM_API_KEY.trim();
        if (!buildConfigKey.isEmpty()) {
            return buildConfigKey;
        }
        return DEFAULT_API_KEY;
    }

    public static String getModel(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        String savedModel = prefs.getString(PREF_NIM_MODEL, "");
        if (savedModel != null && !savedModel.trim().isEmpty()) {
            return savedModel.trim();
        }
        String buildConfigModel = BuildConfig.NVIDIA_NIM_MODEL == null ? "" : BuildConfig.NVIDIA_NIM_MODEL.trim();
        return buildConfigModel.isEmpty() ? DEFAULT_MODEL : buildConfigModel;
    }

    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE);
        String savedBaseUrl = prefs.getString(PREF_NIM_BASE_URL, "");
        if (savedBaseUrl != null && !savedBaseUrl.trim().isEmpty()) {
            return normalizeBaseUrl(savedBaseUrl.trim());
        }
        String buildConfigBaseUrl = BuildConfig.NVIDIA_NIM_BASE_URL == null ? "" : BuildConfig.NVIDIA_NIM_BASE_URL.trim();
        return normalizeBaseUrl(buildConfigBaseUrl.isEmpty() ? DEFAULT_BASE_URL : buildConfigBaseUrl);
    }

    public static void generateQuestions(Context context, String prompt, NimCallback callback) {
        performNimRequest(context, prompt, 220, 0.35, callback);
    }

    public static void generateAnswer(Context context, String prompt, NimCallback callback) {
        performNimRequest(context, prompt, DESC_MAX_TOKENS, DESC_TEMPERATURE, callback);
    }

    private static void performNimRequest(Context context, String prompt, int maxTokens, double temperature, NimCallback callback) {
        String apiKey = getApiKey(context);
        if (apiKey.isEmpty()) {
            callback.onError("NVIDIA NIM API key is not configured.");
            return;
        }

        String model = getModel(context);
        String endpoint = getBaseUrl(context) + "/chat/completions";

        try {
            JSONObject requestJson = buildChatCompletionsPayload(prompt, model, maxTokens, temperature);
            executeWithRetry(endpoint, apiKey, requestJson, callback, 0);
        } catch (JSONException e) {
            callback.onError("Could not create NVIDIA NIM request: " + e.getMessage());
        }
    }

    private static void executeWithRetry(String endpoint,
                                         String apiKey,
                                         JSONObject requestJson,
                                         NimCallback callback,
                                         int attempt) {
        RequestBody body = RequestBody.create(requestJson.toString(), JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (attempt < MAX_RETRIES) {
                    scheduleRetry(endpoint, apiKey, requestJson, callback, attempt + 1);
                } else {
                    callback.onError("NVIDIA NIM request failed: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() == null ? "" : response.body().string();
                int responseCode = response.code();
                response.close();

                if (!response.isSuccessful()) {
                    if (isRetryableHttpCode(responseCode) && attempt < MAX_RETRIES) {
                        scheduleRetry(endpoint, apiKey, requestJson, callback, attempt + 1);
                        return;
                    }
                    callback.onError("NVIDIA NIM returned HTTP " + responseCode + ": " + responseBody);
                    return;
                }

                try {
                    callback.onSuccess(parseOpenAiCompatibleResult(responseBody));
                } catch (JSONException e) {
                    callback.onError("Could not read NVIDIA NIM response: " + e.getMessage());
                }
            }
        });
    }

    private static boolean isRetryableHttpCode(int responseCode) {
        return responseCode == 408 || responseCode == 409 || responseCode == 425 || responseCode == 429 || responseCode >= 500;
    }

    private static void scheduleRetry(String endpoint,
                                      String apiKey,
                                      JSONObject requestJson,
                                      NimCallback callback,
                                      int nextAttempt) {
        long delayMs = RETRY_BASE_SLEEP_MS * (1L << Math.min(nextAttempt - 1, 4));
        retryHandler.postDelayed(() -> executeWithRetry(endpoint, apiKey, requestJson, callback, nextAttempt), delayMs);
    }

    private static JSONObject buildChatCompletionsPayload(String prompt, String model, int maxTokens, double temperature) throws JSONException {
        JSONObject payload = new JSONObject();
        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "You are SkyShelf's concise weather insight writer. Use only the provided weather data. " +
                        "Write practical, safety-aware advice in 3 to 5 short sentences. No markdown.");
        messages.put(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);

        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);
        payload.put("top_p", 1.0);
        payload.put("stream", false);
        return payload;
    }

    private static String parseOpenAiCompatibleResult(String responseBody) throws JSONException {
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray choices = jsonResponse.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new JSONException("No choices returned.");
        }

        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.optJSONObject("message");
        if (message != null) {
            String content = message.optString("content", "").trim();
            if (!content.isEmpty()) {
                return content;
            }
        }

        String text = firstChoice.optString("text", "").trim();
        if (!text.isEmpty()) {
            return text;
        }
        throw new JSONException("No answer text returned.");
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? DEFAULT_BASE_URL : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            normalized = normalized.substring(0, normalized.length() - "/chat/completions".length());
        }
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }
}
