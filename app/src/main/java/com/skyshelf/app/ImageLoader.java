package com.skyshelf.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ImageLoader {
    private static final OkHttpClient client = new OkHttpClient();
    private static final LruCache<String, Bitmap> cache = new LruCache<>(20);

    private ImageLoader() {
    }

    public static void load(String imageUrl, ImageView imageView, int placeholderResourceId) {
        imageView.setImageResource(placeholderResourceId);
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        String normalizedUrl = imageUrl.trim();
        imageView.setTag(normalizedUrl);
        Bitmap cachedBitmap = cache.get(normalizedUrl);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        Request request = new Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", "SkyShelf/1.0 (Android)")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Keep placeholder image. The city still remains usable.
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    response.close();
                    return;
                }

                Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                response.close();
                if (bitmap == null) {
                    return;
                }

                cache.put(normalizedUrl, bitmap);
                imageView.post(() -> {
                    Object tag = imageView.getTag();
                    if (normalizedUrl.equals(tag)) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }
}
