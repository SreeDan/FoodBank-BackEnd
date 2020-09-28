package com.sree.foodbank.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Date;

public class CompanyRequest {
    private final int requestId;
    private final String requesterToken;
    private final int receiverId;
    private final String[] food;
    private final Date date;
    private final String type;
    private final String status;

    public CompanyRequest(@JsonProperty("token") String requesterToken,
                          @JsonProperty("requestId") int requestId,
                          @JsonProperty("receiverId") int receiverId,
                          @JsonProperty("food") String[] food,
                          @JsonProperty("date") Date date,
                          @JsonProperty("type") String type,
                          @JsonProperty("status") String status) {
        this.requestId = requestId;
        this.requesterToken = requesterToken;
        this.receiverId = receiverId;
        this.food = food;
        this.date = date;
        this.type = type;
        this.status = status;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getRequesterToken() {
        return requesterToken;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public String[] getFood() {
        return food;
    }

    public Date getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }
}
