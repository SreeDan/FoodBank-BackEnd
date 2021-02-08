package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Location {
    private final double lat;
    private final double lng;

    public Location(@JsonProperty("lat") double lat,
                    @JsonProperty("lng") double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
