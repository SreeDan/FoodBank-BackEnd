package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import java.util.UUID;

public class Company {
    private final UUID id;
    private final String food;

    @NotBlank
    private final String name;

    public Company(@JsonProperty("id") UUID id,
                   @JsonProperty("name") String name,
                   @JsonProperty("food") String food) {
        this.id = id;
        this.name = name;
        this.food = food;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFood() {
        return food;
    }
}
