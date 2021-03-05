package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Token {
    private final String token;
    private final String email;

    public Token(@JsonProperty("token") String token,
                 @JsonProperty("email") String email) {
        this.token = token;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }
}
