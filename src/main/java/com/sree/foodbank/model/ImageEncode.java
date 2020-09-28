package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Base64;

public class ImageEncode {
    private final String token;
    private final String base64;

    public ImageEncode(@JsonProperty("token") String token,
                       @JsonProperty("base") String base64) {
        this.token = token;
        this.base64 = base64;
    }

    public String getToken() {
        return token;
    }

    public String getBase64() {
        return base64;
    }
}
