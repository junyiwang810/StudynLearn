package com.example.service;

import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;

import java.io.IOException;

public class RewardService {
    private final GoalRepository goalRepository;
    private final SettingsRepository settingsRepository;

    public RewardService(GoalRepository goalRepository, SettingsRepository settingsRepository) {
        this.goalRepository = goalRepository;
        this.settingsRepository = settingsRepository;
    }

    public String applyRewardForStudySession() {
        String mainGoalName = settingsRepository.getString("currentGoalName", "");
        if (mainGoalName.isEmpty()) {
            return "No main goal set!";
        }

        double hourlyRate = settingsRepository.getDouble("hourlyRate", 0.0);
        int durationSeconds = settingsRepository.getInt(
            "studyDurationSeconds",
            settingsRepository.getInt("studyDuration", 25) * 60
        );
        double earned = (durationSeconds / 3600.0) * hourlyRate;

        try {
            boolean updated = goalRepository.addAmountToGoal(mainGoalName, earned);
            if (!updated) {
                return "Goal not found.";
            }
            return String.format("Earned $%.2f!", earned);
        } catch (IOException ignored) {
            return "Could not update goal progress.";
        }
    }
}
