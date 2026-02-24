package com.example.service;

public class RewardResult {
    private final String message;
    private final String mainGoalName;
    private final String purchaseLink;
    private final double overflowAmount;
    private final boolean goalCompleted;
    private final boolean success;

    public RewardResult(
        String message,
        String mainGoalName,
        String purchaseLink,
        double overflowAmount,
        boolean goalCompleted,
        boolean success
    ) {
        this.message = message;
        this.mainGoalName = mainGoalName == null ? "" : mainGoalName;
        this.purchaseLink = purchaseLink == null ? "" : purchaseLink;
        this.overflowAmount = overflowAmount;
        this.goalCompleted = goalCompleted;
        this.success = success;
    }

    public static RewardResult error(String message) {
        return new RewardResult(message, "", "", 0.0, false, false);
    }

    public String getMessage() {
        return message;
    }

    public String getMainGoalName() {
        return mainGoalName;
    }

    public String getPurchaseLink() {
        return purchaseLink;
    }

    public double getOverflowAmount() {
        return overflowAmount;
    }

    public boolean isGoalCompleted() {
        return goalCompleted;
    }

    public boolean isSuccess() {
        return success;
    }
}
