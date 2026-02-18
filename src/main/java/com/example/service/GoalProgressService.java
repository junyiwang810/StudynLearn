package com.example.service;

import com.example.model.Goal;
import com.example.model.ProgressInfo;
import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;

import java.io.IOException;
import java.util.Optional;

public class GoalProgressService {
    private final GoalRepository goalRepository;
    private final SettingsRepository settingsRepository;

    public GoalProgressService(GoalRepository goalRepository, SettingsRepository settingsRepository) {
        this.goalRepository = goalRepository;
        this.settingsRepository = settingsRepository;
    }

    public ProgressInfo getMainGoalProgress() {
        String mainGoalName = settingsRepository.getString("currentGoalName", "");
        if (mainGoalName.isEmpty()) {
            return ProgressInfo.empty("");
        }

        Optional<Goal> mainGoal = goalRepository.findByName(mainGoalName);
        if (!mainGoal.isPresent()) {
            return ProgressInfo.empty(mainGoalName);
        }

        Goal goal = mainGoal.get();
        int percent = 0;
        if (goal.getCost() > 0) {
            percent = (int) ((goal.getCurrentAmount() / goal.getCost()) * 100);
        }
        return new ProgressInfo(goal.getName(), percent, goal.getCurrentAmount(), goal.getCost());
    }

    public String getMainGoalName() {
        return settingsRepository.getString("currentGoalName", "");
    }

    public void setMainGoalName(String goalName) throws IOException {
        settingsRepository.setProperty("currentGoalName", goalName, "Updated Main Goal");
    }
}
