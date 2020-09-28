package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompanyNeededFood {
    private final String[] neededFood;

    public CompanyNeededFood(@JsonProperty("neededFood") String[] neededFood) {
        this.neededFood = neededFood;
    }

    public String[] getNeededFood() {
        return neededFood;
    }
}
