package com.skyshelf.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder> {

    public interface SearchResultListener {
        void onResultClicked(City city);
        void onMoreClicked(View anchor, City city);
    }

    private final ArrayList<City> items = new ArrayList<>();
    private final SearchResultListener listener;

    public SearchResultAdapter(SearchResultListener listener) {
        this.listener = listener;
    }

    public void setItems(ArrayList<City> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public ArrayList<City> getItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        City city = items.get(position);
        holder.title.setText(city.getName());
        holder.subtitle.setText(cleanSubtitle(holder.itemView.getContext(), city.getSubtitle()));
        if (city.getImageUrl() != null && city.getImageUrl().startsWith("http")) {
            ImageLoader.load(city.getImageUrl(), holder.image, city.getImageResourceId());
        } else {
            holder.image.setImageResource(city.getImageResourceId());
        }

        holder.itemView.setOnClickListener(v -> listener.onResultClicked(city));
        holder.moreButton.setOnClickListener(v -> listener.onMoreClicked(v, city));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String cleanSubtitle(Context context, String subtitle) {
        if (subtitle == null || subtitle.trim().isEmpty()) {
            return context.getString(R.string.weather_collection);
        }
        return subtitle.replace(" • " + context.getString(R.string.weather_collection), "").trim();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView subtitle;
        final ImageButton moreButton;

        ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.searchResultImage);
            title = itemView.findViewById(R.id.searchResultTitle);
            subtitle = itemView.findViewById(R.id.searchResultSubtitle);
            moreButton = itemView.findViewById(R.id.searchResultMore);
        }
    }
}
