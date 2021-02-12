package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompanyInfo {
    private final Integer id;
    private final String user;
    private final String password;
    private final String name;
    private final String url;
    private final String phone;
    private final Object neededFood;
    private final Object availableFood;
    private final Object address;
    private final String billing;
    private final String city;
    private final String state;
    private final String ZIP;
    private final String userType;
    private final String image;
    private final String email;
    private final Double lat;
    private final Double lng;

    public CompanyInfo(@JsonProperty("id") Integer id,
                       @JsonProperty("user") String user,
                       @JsonProperty("pass") String password,
                       @JsonProperty("name") String name,
                       @JsonProperty("url") String url,
                       @JsonProperty("phone") String phone,
                       @JsonProperty("neededFood") Object neededFood,
                       @JsonProperty("availableFood") Object availableFood,
                       @JsonProperty("address") Object address,
                       @JsonProperty("billing") String billing,
                       @JsonProperty("city") String city,
                       @JsonProperty("state") String state,
                       @JsonProperty("ZIP") String ZIP,
                       @JsonProperty("userType") String userType,
                       @JsonProperty("image") String image,
                       @JsonProperty("email") String email,
                       @JsonProperty("lat") Double lat,
                       @JsonProperty("lng") Double lng) {
        this.id = id;
        this.user = user;
        this.password = password;
        this.name = name;
        this.url = url;
        this.phone = phone;
        this.neededFood = neededFood;
        this.availableFood = availableFood;
        this.address = address;
        this.userType = userType;
        this.image = image;
        this.email = email;
        this.billing = billing;
        this.city = city;
        this.state = state;
        this.ZIP = ZIP;
        this.lat = lat;
        this.lng = lng;
    }

    public Integer getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
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

    public String getUserType() {
        return userType;
    }

    public String getImage() {
        return image;
    }

    public String getEmail() {
        return email;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }
}
