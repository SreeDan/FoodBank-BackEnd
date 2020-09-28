package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompanyFilter {
    private final String type;
    private final String[] availableFood;
    private final String[] neededFood;

    public CompanyFilter(@JsonProperty("type") String type,
                         @JsonProperty("availableFood") String[] availableFood,
                         @JsonProperty("neededFood") String[] neededFood) {
        this.type = type;
        this.availableFood = availableFood;
        this.neededFood = neededFood;

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
}
