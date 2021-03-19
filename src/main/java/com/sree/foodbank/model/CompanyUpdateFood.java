package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CompanyUpdateFood {
    private final List<Food> food;
    private final String type;

    public CompanyUpdateFood(@JsonProperty("food") List<Food> food,
                             @JsonProperty("type") String type) {
        this.food = food;
        this.type = type;
    }

    public List<Food> getFood() {
        return food;
    }

    public String getType() {
        return type;
    }
}
