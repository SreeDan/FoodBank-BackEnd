package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class CompanyReturnRequest {
    private final String token;
    private final int requestId;
    private final String requesterName;
    private final String receiverName;
    private final String[] food;
    private final String date;
    private final String type;
    private final String status;
    //create table accepted, processing, declined, ready, delivered

    public CompanyReturnRequest(@JsonProperty("token") String token,
                                @JsonProperty("requestId") int requestId,
                                @JsonProperty("requesterName") String requesterName,
                                @JsonProperty("receiverName") String receiverName,
                                @JsonProperty("food") String[] food,
                                @JsonProperty("date") String date,
                                @JsonProperty("userType") String type,
                                @JsonProperty("status") String status) {
        this.token = token;
        this.requestId = requestId;
        this.requesterName = requesterName;
        this.receiverName = receiverName;
        this.food = food;
        this.date = date;
        this.type = type;
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String[] getFood() {
        return food;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }
}
