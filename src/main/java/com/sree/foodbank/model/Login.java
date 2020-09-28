package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Login {
    private final String username;
    private final String password;
    public Login(@JsonProperty("user") String username,
                 @JsonProperty("password") String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
