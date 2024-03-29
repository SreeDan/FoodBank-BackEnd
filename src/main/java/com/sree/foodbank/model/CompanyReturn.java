package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class CompanyReturn {

    private final Integer id;
    private final String name;
    private final String url;
    private final String phone;
    private final Object neededFood;
    private final Object availableFood;
    private final Object address;
    private final String userType;
    private final String image;
    private final String email;
    private final Double distance;
    private final Double lat;
    private final Double lng;

    public CompanyReturn(@JsonProperty("id") Integer id,
                         @JsonProperty("name") String name,
                         @JsonProperty("url") String url,
                         @JsonProperty("phone") String phone,
                         @JsonProperty("neededFood") Object neededFood,
                         @JsonProperty("availableFood") Object availableFood,
                         @JsonProperty("address") Object address,
                         @JsonProperty("userType") String userType,
                         @JsonProperty("image") String image,
                         @JsonProperty("email") String email,
                         @JsonProperty("distance") Double distance,
                         @JsonProperty("lat") Double lat,
                         @JsonProperty("lng") Double lng) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.phone = phone;
        this.neededFood = neededFood;
        this.availableFood = availableFood;
        this.address = address;
        this.userType = userType;
        this.image = image;
        this.email = email;
        this.distance = distance;
        this.lat = lat;
        this.lng = lng;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getPhone() {
        return phone;
    }

    public Object getNeededFood() {
        return neededFood;
    }

    public Object getAvailableFood() {
        return availableFood;
    }

    public Object getAddress() {
        return address;
    }

    public String getUserType() {
        return userType;
    }

    public String getImage() {
        return image;
    }

    public String getEmail() {
        return email;
    }

    public Double getDistance() {
        return distance;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }
}
