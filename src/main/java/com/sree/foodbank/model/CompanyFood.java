package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompanyFood {
    private final Object food;

    public CompanyFood(@JsonProperty("food") Object food) {
        this.food = food;
    }

    public Object getFood() {
        return food;
    }
}
