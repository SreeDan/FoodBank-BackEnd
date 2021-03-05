package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateAccount {
    private final String token;
    private final String email;
    private final String username;
    private final String password;
    private final String name;
    private final String type;
    private final String billing;
    private final String city;
    private final String state;
    private final String ZIP;

    public CreateAccount(@JsonProperty("token") String token,
                         @JsonProperty("email") String email,
                         @JsonProperty("username") String username,
                         @JsonProperty("password") String password,
                         @JsonProperty("name") String name,
                         @JsonProperty("type") String type,
                         @JsonProperty("billing") String billing,
                         @JsonProperty("city") String city,
                         @JsonProperty("state") String state,
                         @JsonProperty("ZIP") String ZIP) {
        this.token = token;
        this.email = email;
        this.username = username;
        this.password = password;
        this.name = name;
        this.type = type;
        this.billing = billing;
        this.city = city;
        this.state = state;
        this.ZIP = ZIP;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getBilling() {
        return billing;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZIP() {
        return ZIP;
    }
}
