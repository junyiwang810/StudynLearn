package com.example.controller;

import com.example.App;
import com.example.model.ProgressInfo;
import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;
import com.example.service.GoalProgressService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;

public class PrimaryController {

    @FXML private Label percentageLabel;
    @FXML private Label fractionLabel;
    @FXML private Label goalNameLabel;

    private final GoalProgressService goalProgressService = new GoalProgressService(
        new GoalRepository(),
        new SettingsRepository()
    );

    @FXML
    public void initialize() {
        ProgressInfo info = goalProgressService.getMainGoalProgress();
        String goalName = info.getGoalName().isEmpty() ? "No Main Goal" : info.getGoalName();

        goalNameLabel.setText(goalName);
        percentageLabel.setText(info.getPercent() + "% Complete");
        fractionLabel.setText(String.format("$%.2f / $%.2f", info.getCurrent(), info.getCost()));
    }

    @FXML
    private void switchToStudying() throws IOException {
        App.setRoot("startStudying");
    }

    @FXML
    private void switchToGoals() throws IOException {
        App.setRoot("goals");
    }

    @FXML
    private void switchToSettings() throws IOException {
        App.setRoot("settings");
    }
}
