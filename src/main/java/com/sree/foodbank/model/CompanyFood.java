package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompanyFood {
    private final BigDecimal id;
    private final String token;
    private final String[] neededFood;
    private final String[] availableFood;

//    @NotBlank
//    private final String name;

    public CompanyFood(@JsonProperty("id") BigDecimal id,
                       @JsonProperty("token") String token,
                       @JsonProperty("neededFood") String[] neededFood,
                       @JsonProperty("availableFood") String[] availableFood) {
        this.id = id;
        this.token = token;
        this.neededFood = neededFood;
        this.availableFood = availableFood;
    }

    public BigDecimal getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    /*
    public String getName() {
        return name;
    }
*/
    public String[] getNeededFood() {
        return neededFood;
    }

    public String[] getAvailableFood() {
        return availableFood;
    }
}
