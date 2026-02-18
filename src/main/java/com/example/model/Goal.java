package com.example.model;

public class Goal {
    private final String name;
    private final double cost;
    private final double currentAmount;

    public Goal(String name, double cost, double currentAmount) {
        this.name = name == null ? "" : name.trim();
        this.cost = cost;
        this.currentAmount = currentAmount;
    }

    public String getName() {
        return name;
    }

    public double getCost() {
        return cost;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }
}
