package com.sree.foodbank.model;

public class Distance {
    private final int id;
    private final Double distance;

    public Distance(int id, Double distance) {
        this.id = id;
        this.distance = distance;
    }

    public int getId() {
        return id;
    }

    public Double getDistance() {
        return distance;
    }
}
