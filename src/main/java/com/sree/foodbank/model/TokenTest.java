package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenTest {
    private final String token;

    public TokenTest(@JsonProperty("token") String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
