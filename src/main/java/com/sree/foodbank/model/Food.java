package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class Food {
    private final BigDecimal id;
    private final String food;

    public Food(@JsonProperty("value") BigDecimal id,
                @JsonProperty("label") String food) {
        this.id = id;
        this.food = food;
    }

    public BigDecimal getId() {
        return id;
    }

    public String getFood() {
        return food;
    }
}
