package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenBody {
    private final String test;

    public TokenBody(@JsonProperty("test") String test) {
        this.test = test;
    }

    public String getTest() {
        return test;
    }
}
