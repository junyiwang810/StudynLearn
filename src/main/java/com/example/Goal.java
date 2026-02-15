package com.example;

public class Goal {
    public String name;
    public double cost;
    public double currentAmount;

    public Goal(String name, double cost, double currentAmount) {
        this.name = name;
        this.cost = cost;
        this.currentAmount = currentAmount;
    }

    @Override
    public String toString() {
        return name + " ($" + cost + ")";
    }
}