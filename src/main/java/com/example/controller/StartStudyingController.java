package com.example.controller;

import com.example.App;
import com.example.repository.SettingsRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

import java.io.IOException;

public class StartStudyingController {

    @FXML private Button startTimerButton;

    private Button selectedDurationButton;
    private Button selectedBreakButton;
    private Button selectedSessionButton;
    private int studyDurationSeconds;
    private int breakDurationSeconds;
    private int sessions;

    private final SettingsRepository settingsRepository = new SettingsRepository();

    @FXML
    public void initialize() {
        updateStartTimerButtonState();
    }

    @FXML
    private void handleDuration(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        selectedDurationButton = setSelectedButton(selectedDurationButton, clicked);
        studyDurationSeconds = parseButtonDurationToSeconds(clicked.getText());
        updateStartTimerButtonState();
    }

    @FXML
    private void handleBreak(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        selectedBreakButton = setSelectedButton(selectedBreakButton, clicked);
        breakDurationSeconds = parseButtonDurationToSeconds(clicked.getText());
        updateStartTimerButtonState();
    }

    @FXML
    private void handleSessions(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        selectedSessionButton = setSelectedButton(selectedSessionButton, clicked);
        sessions = Integer.parseInt(clicked.getText().trim());
        updateStartTimerButtonState();
    }

    @FXML
    private void startTimer() throws IOException {
        if (studyDurationSeconds <= 0 || breakDurationSeconds <= 0 || sessions <= 0) {
            showAlert(
                Alert.AlertType.WARNING,
                "Selection Required",
                "Select study duration, break duration, and number of sessions first."
            );
            return;
        }

        settingsRepository.setProperty("studyDurationSeconds", String.valueOf(studyDurationSeconds), "Timer Settings");
        settingsRepository.setProperty("breakDurationSeconds", String.valueOf(breakDurationSeconds), "Timer Settings");
        settingsRepository.setProperty("sessions", String.valueOf(sessions), "Timer Settings");
        App.setRoot("timer");
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    private Button setSelectedButton(Button previous, Button next) {
        if (previous != null) {
            previous.getStyleClass().remove("selected-chip");
        }
        if (!next.getStyleClass().contains("selected-chip")) {
            next.getStyleClass().add("selected-chip");
        }
        return next;
    }

    private void updateStartTimerButtonState() {
        if (startTimerButton == null) {
            return;
        }
        boolean ready = studyDurationSeconds > 0 && breakDurationSeconds > 0 && sessions > 0;
        startTimerButton.setDisable(!ready);
    }

    private int parseButtonDurationToSeconds(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 0) {
            return 0;
        }

        int value;
        try {
            value = Integer.parseInt(parts[0]);
        } catch (NumberFormatException ignored) {
            return 0;
        }

        String unit = parts.length > 1 ? parts[1].toLowerCase() : "min";
        if (unit.startsWith("sec")) {
            return value;
        }
        return value * 60;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());
        alert.showAndWait();
    }
}
