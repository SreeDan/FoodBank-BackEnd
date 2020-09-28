package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonObject;

import java.util.ArrayList;


public class Test {
    private final String[] availableFood;
    private final String[] neededFood;

    public Test(@JsonProperty("availableFood") String[] availableFood,
                @JsonProperty("neededFood") String[] neededFood) {
        this.availableFood = availableFood;
        this.neededFood = neededFood;
    }

    public String[] getAvaialblefood() {
        return availableFood;
    }

    public String[] getNeededFood() {
        return neededFood;
    }
}
