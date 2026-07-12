package com.skyshelf.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.CityViewHolder> {

    private final ArrayList<City> cities = new ArrayList<>();
    private static final Map<String, String> imageCache = new HashMap<>();

    public static class CityViewHolder extends RecyclerView.ViewHolder {
        private final View coverCard;
        private final ImageView imageViewCity;
        private final TextView textViewCityName;
        private final TextView textViewCitySubtitle;

        public CityViewHolder(@NonNull View itemView) {
            super(itemView);
            coverCard = itemView.findViewById(R.id.coverCard);
            imageViewCity = itemView.findViewById(R.id.imageViewCity);
            textViewCityName = itemView.findViewById(R.id.textViewCityName);
            textViewCitySubtitle = itemView.findViewById(R.id.textViewCitySubtitle);
        }
    }

    public void setItems(ArrayList<City> newCities) {
        cities.clear();
        cities.addAll(newCities);
        notifyDataSetChanged();
    }

    public ArrayList<City> getItems() {
        return new ArrayList<>(cities);
    }

    @NonNull
    @Override
    public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city, parent, false);
        return new CityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
        City currentCity = cities.get(position);
        makeCoverSquare(holder);
        bindCityImage(holder, currentCity);
        holder.textViewCityName.setText(currentCity.getName());
        holder.textViewCitySubtitle.setText(cleanSubtitle(holder.itemView.getContext(), currentCity.getSubtitle()));

        View.OnClickListener openDetails = v -> openDetails(v.getContext(), currentCity);
        holder.itemView.setOnClickListener(openDetails);
        holder.imageViewCity.setOnClickListener(openDetails);
    }

    private void makeCoverSquare(CityViewHolder holder) {
        holder.coverCard.post(() -> {
            int width = holder.coverCard.getWidth();
            if (width <= 0) {
                return;
            }
            ViewGroup.LayoutParams params = holder.coverCard.getLayoutParams();
            if (params.height != width) {
                params.height = width;
                holder.coverCard.setLayoutParams(params);
            }
        });
    }

    private String cleanSubtitle(Context context, String subtitle) {
        if (subtitle == null || subtitle.trim().isEmpty()) {
            return context.getString(R.string.weather_collection);
        }
        String cleaned = subtitle.replace(
                context.getString(R.string.weather_collection),
                context.getString(R.string.weather_short)
        ).trim();
        if (cleaned.length() > 42) {
            return cleaned.substring(0, 39).trim() + "…";
        }
        return cleaned;
    }

    private void bindCityImage(CityViewHolder holder, City currentCity) {
        String imageUrl = currentCity.getImageUrl();
        String cacheKey = currentCity.getName().toLowerCase(Locale.US);
        holder.imageViewCity.setTag(cacheKey);

        if (imageUrl != null && imageUrl.startsWith("http")) {
            ImageLoader.load(imageUrl, holder.imageViewCity, currentCity.getImageResourceId());
            return;
        }

        if (imageCache.containsKey(cacheKey)) {
            ImageLoader.load(imageCache.get(cacheKey), holder.imageViewCity, currentCity.getImageResourceId());
            return;
        }

        holder.imageViewCity.setImageResource(R.drawable.ic_placeholder);
        WeatherRepository.lookupCity(currentCity.getName(), new WeatherRepository.LocationCallback() {
            @Override
            public void onSuccess(WeatherRepository.LocationResult locationResult) {
                CityImageRepository.fetchCityImage(locationResult, new CityImageRepository.ImageCallback() {
                    @Override
                    public void onSuccess(String fetchedImageUrl) {
                        imageCache.put(cacheKey, fetchedImageUrl);
                        holder.imageViewCity.post(() -> {
                            Object tag = holder.imageViewCity.getTag();
                            if (tag != null && tag.equals(cacheKey)) {
                                ImageLoader.load(fetchedImageUrl, holder.imageViewCity, currentCity.getImageResourceId());
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        // keep placeholder
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                // keep placeholder
            }
        });
    }

    private void openDetails(Context context, City city) {
        Intent intent = new Intent(context, DetailsActivity.class);
        intent.putExtra("cityName", city.getName());
        intent.putExtra("citySubtitle", city.getSubtitle());
        intent.putExtra("imageUrl", city.getImageUrl() == null ? "" : city.getImageUrl());
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return cities.size();
    }

    public void addItem(City city) {
        cities.add(city);
        notifyItemInserted(cities.size() - 1);
        triggerSave();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < cities.size()) {
            cities.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, cities.size() - position);
            triggerSave();
        }
    }

    public interface OnDataChangeListener {
        void onDataChanged(ArrayList<City> updatedCities);
    }

    private OnDataChangeListener onDataChangeListener;

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.onDataChangeListener = listener;
    }

    private void triggerSave() {
        if (onDataChangeListener != null) {
            onDataChangeListener.onDataChanged(new ArrayList<>(cities));
        }
    }
}
