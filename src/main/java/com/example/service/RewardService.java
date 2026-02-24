package com.example.service;

import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;
import com.example.model.Goal;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RewardService {
    private final GoalRepository goalRepository;
    private final SettingsRepository settingsRepository;

    public RewardService(GoalRepository goalRepository, SettingsRepository settingsRepository) {
        this.goalRepository = goalRepository;
        this.settingsRepository = settingsRepository;
    }

    public RewardResult applyRewardForStudySession() {
        String mainGoalName = settingsRepository.getString("currentGoalName", "");
        if (mainGoalName.isEmpty()) {
            return RewardResult.error("No main goal set!");
        }

        double hourlyRate = settingsRepository.getDouble("hourlyRate", 0.0);
        double balance = settingsRepository.getDouble("balance", 0.0);
        double studyHourlyRate = StudyRateService.calculate(hourlyRate, balance);
        int durationSeconds = settingsRepository.getInt(
            "studyDurationSeconds",
            settingsRepository.getInt("studyDuration", 25) * 60
        );
        double earned = (durationSeconds / 3600.0) * studyHourlyRate;

        try {
            List<Goal> goals = goalRepository.loadGoals();
            if (goals.isEmpty()) {
                return RewardResult.error("Goal not found.");
            }

            Goal mainGoal = null;
            for (Goal goal : goals) {
                if (goal.getName().equals(mainGoalName)) {
                    mainGoal = goal;
                    break;
                }
            }
            if (mainGoal == null) {
                return RewardResult.error("Goal not found.");
            }

            double previousAmount = mainGoal.getCurrentAmount();
            double updatedAmount = previousAmount + earned;
            double cappedAmount = Math.min(updatedAmount, mainGoal.getCost());
            double overflow = Math.max(0.0, updatedAmount - mainGoal.getCost());
            boolean goalCompleted = mainGoal.getCost() > 0 && cappedAmount >= mainGoal.getCost();

            List<Goal> updatedGoals = new ArrayList<>();
            for (Goal goal : goals) {
                if (goal.getName().equals(mainGoalName)) {
                    updatedGoals.add(new Goal(goal.getName(), goal.getCost(), cappedAmount));
                } else {
                    updatedGoals.add(goal);
                }
            }
            goalRepository.saveGoals(updatedGoals);

            String link = settingsRepository.getString(goalLinkKey(mainGoalName), "");
            String message = String.format("Earned $%.2f!", earned);
            return new RewardResult(message, mainGoalName, link, overflow, goalCompleted, true);
        } catch (IOException ignored) {
            return RewardResult.error("Could not update goal progress.");
        }
    }

    public static String goalLinkKey(String goalName) {
        return "goal.link." + URLEncoder.encode(goalName == null ? "" : goalName, StandardCharsets.UTF_8);
    }
}
