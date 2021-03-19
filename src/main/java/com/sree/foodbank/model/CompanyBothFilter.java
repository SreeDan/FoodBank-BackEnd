package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompanyBothFilter {
    private final String type;
    private final String[] availableFood;
    private final String[] neededFood;
    private final Double lat;
    private final Double lng;
    private final String state;

    public CompanyBothFilter(@JsonProperty("type") String type,
                             @JsonProperty("availableFood") String[] availableFood,
                             @JsonProperty("neededFood") String[] neededFood,
                             @JsonProperty("lat") Double lat,
                             @JsonProperty("lng") Double lng,
                             @JsonProperty("state") String state) {
        this.type = type;
        this.availableFood = availableFood;
        this.neededFood = neededFood;
        this.lat = lat;
        this.lng = lng;
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public String[] getAvailableFood() {
        return availableFood;
    }

    public String[] getNeededFood() {
        return neededFood;
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
}
