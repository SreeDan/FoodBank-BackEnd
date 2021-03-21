package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Location {
    private final Double lat;
    private final Double lng;
    private final String state;
    private final Boolean standard;

    public Location(@JsonProperty("lat") Double lat,
                    @JsonProperty("lng") Double lng,
                    @JsonProperty("state") String state,
                    @JsonProperty("default") Boolean standard) {
        this.lat = lat;
        this.lng = lng;
        this.state = state;
        this.standard = standard;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public String getState() {
        return state;
    }

    public Boolean getStandard() {
        return standard;
    }
}
