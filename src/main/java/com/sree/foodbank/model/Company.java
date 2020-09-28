package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class Company {
    private final BigDecimal id;
    private final Object address;
    private final String token;
//    private final String food;

    @NotBlank
    private final String name;
    private final String userType;

    public Company(@JsonProperty("id") BigDecimal id,
                   @JsonProperty("name") String name,
                   @JsonProperty("address") Object address,
                   @JsonProperty("token") String token,
                   @JsonProperty("userType") String userType) {
//                   @JsonProperty("food") String food) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.token = token;
        this.userType = userType;
//        this.food = food;
    }

    public BigDecimal getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object getAddress() {
        return address;
    }

    public String getToken() {
        return token;
    }

    public String getUserType() {
        return userType;
    }
/*    public String getFood() {
        return food;
    }
 */
}
