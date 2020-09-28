package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompanyReturnFood {
    private final String name;
    private final String[] food;

    public CompanyReturnFood(@JsonProperty("name") String name,
                             @JsonProperty("food") String[] food) {
        this.name = name;
        this.food = food;
    }

    public String getName() {
        return name;
    }

    public String[] getFood() {
        return food;
    }
}
