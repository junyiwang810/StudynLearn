package com.example.model;

public class ProgressInfo {
    private final String goalName;
    private final int percent;
    private final double current;
    private final double cost;

    public ProgressInfo(String goalName, int percent, double current, double cost) {
        this.goalName = goalName;
        this.percent = percent;
        this.current = current;
        this.cost = cost;
    }

    public String getGoalName() {
        return goalName;
    }

    public int getPercent() {
        return percent;
    }

    public double getCurrent() {
        return current;
    }

    public double getCost() {
        return cost;
    }

    public static ProgressInfo empty(String goalName) {
        return new ProgressInfo(goalName, 0, 0.0, 0.0);
    }
}
