package com.skyshelf.app;

import java.io.Serializable;

public class City implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_NAME = "Unknown city";
    private static final String DEFAULT_SUBTITLE = "Weather collection";
    private final String name;
    private final String subtitle;
    private final String imageUrl;
    private final int imageResourceId;

    public City(String name, String imageUrl) {
        this(name, DEFAULT_SUBTITLE, imageUrl, R.drawable.ic_placeholder);
    }

    public City(String name, String subtitle, String imageUrl) {
        this(name, subtitle, imageUrl, R.drawable.ic_placeholder);
    }

    public City(String name, int imageResourceId) {
        this(name, DEFAULT_SUBTITLE, null, imageResourceId);
    }

    public City(String name, String subtitle, String imageUrl, int imageResourceId) {
        this.name = name == null ? DEFAULT_NAME : name;
        this.subtitle = subtitle == null || subtitle.trim().isEmpty() ? DEFAULT_SUBTITLE : subtitle.trim();
        this.imageUrl = imageUrl == null || imageUrl.trim().isEmpty() ? null : imageUrl.trim();
        this.imageResourceId = imageResourceId == 0 ? R.drawable.ic_placeholder : imageResourceId;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }
}
