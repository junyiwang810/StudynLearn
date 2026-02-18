package com.example;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.util.Duration;
import org.openqa.selenium.WebDriver;

import java.io.*;
import java.util.*;

public class TimerController {

    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Button pauseButton;

    private int timeSeconds;
    private Timeline timeline;
    private boolean isManuallyPaused = false;
    private WebDriver studyDriver; // Assuming this is managed elsewhere or initialized
    private List<String> whitelist = new ArrayList<>();
    private List<String> blacklist = new ArrayList<>();
    private boolean browserBlocked = false;
    
    private int studyDurationMinutes;
    private int breakDurationMinutes;
    private int totalSessions;
    private int currentSession = 1;
    private boolean isBreak = false;

    @FXML
    public void initialize() {
        loadSettings();
        // Initialize lists or driver here if needed
        startSession();
    }

    private void loadSettings() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
            studyDurationMinutes = Integer.parseInt(props.getProperty("studyDuration", "25"));
            breakDurationMinutes = Integer.parseInt(props.getProperty("breakDuration", "5"));
            totalSessions = Integer.parseInt(props.getProperty("sessions", "1"));
            timeSeconds = studyDurationMinutes;
        } catch (IOException e) {
            timeSeconds = 25 * 60; // Default fallback
        }
    }

    private void startSession() {
        if (statusLabel != null) statusLabel.setText("Studying...");
        updateTimerLabel();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!isManuallyPaused && timeSeconds > 0) {
                timeSeconds--;
                updateTimerLabel();
                if (!isBreak) checkBrowser(); // Only check browser during study
            } else if (timeSeconds <= 0) {
                handlePhaseCompletion();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkBrowser() {
        if (studyDriver != null) {
            try {
                String currentUrl = studyDriver.getCurrentUrl();
                if (currentUrl != null) {
                    String lowerUrl = currentUrl.toLowerCase();
                    boolean isWhitelisted = false;
                    for (String allowed : whitelist) {
                        if (lowerUrl.contains(allowed.toLowerCase())) {
                            isWhitelisted = true;
                            break;
                        }
                    }
                    if (!isWhitelisted) {
                        for (String blocked : blacklist) {
                            if (lowerUrl.contains(blocked.toLowerCase())) {
                                if (statusLabel != null) statusLabel.setText("PAUSED: " + blocked + " detected!");
                                browserBlocked = true;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) { }
        }
    }

    private void updateTimerLabel() {
        if (timerLabel != null) {
            int minutes = timeSeconds / 60;
            int seconds = timeSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
    }

    private void handlePhaseCompletion() {
        timeline.pause();
        if (!isBreak) {
            // Study Session Complete
            processReward();
            
            if (currentSession < totalSessions) {
                // Start Break
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Study Session Complete");
                alert.setHeaderText(null);
                alert.setContentText("Time for a break!");
                alert.showAndWait();

                isBreak = true;
                timeSeconds = breakDurationMinutes * 60;
                if (statusLabel != null) statusLabel.setText("Break Time!");
                updateTimerLabel();
                timeline.play();
            } else {
                // All Sessions Complete
                timeline.stop();
                if (statusLabel != null) statusLabel.setText("All Sessions Complete!");
            }
        } else {
            // Break Complete
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Break Complete");
            alert.setHeaderText(null);
            alert.setContentText("Time to study!");
            alert.showAndWait();

            isBreak = false;
            currentSession++;
            timeSeconds = studyDurationMinutes * 60;
            if (statusLabel != null) statusLabel.setText("Studying (Session " + currentSession + ")...");
            updateTimerLabel();
            timeline.play();
        }
    }

    private void processReward() {
        if (browserBlocked) {
            if (statusLabel != null) statusLabel.setText("Session Failed: Distraction detected.");
            return;
        }

        // Calculate reward
        Properties props = new Properties();
        String mainGoal = "";
        double rate = 0.0;
        int duration = 0;

        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
            mainGoal = props.getProperty("currentGoalName", "");
            rate = Double.parseDouble(props.getProperty("hourlyRate", "0.0"));
            duration = Integer.parseInt(props.getProperty("studyDuration", "0"));
        } catch (IOException e) {
            if (statusLabel != null) statusLabel.setText("Error loading settings");
            return;
        }

        if (mainGoal.isEmpty()) {
            if (statusLabel != null) statusLabel.setText("No main goal set!");
            return;
        }

        double earned = (duration / 60.0) * rate;

        // Update goals.txt
        File file = new File("goals.txt");
        List<String> lines = new ArrayList<>();
        boolean updated = false;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Check if this line corresponds to the main goal
                    if (line.startsWith(mainGoal + ",")) {
                        int lastComma = line.lastIndexOf(',');
                        if (lastComma != -1) {
                            String beforeLast = line.substring(0, lastComma);
                            int secondLastComma = beforeLast.lastIndexOf(',');
                            if (secondLastComma != -1) {
                                try {
                                    String costStr = beforeLast.substring(secondLastComma + 1);
                                    double current = Double.parseDouble(line.substring(lastComma + 1));
                                    
                                    current += earned;
                                    
                                    lines.add(mainGoal + "," + costStr + "," + current);
                                    updated = true;
                                    continue;
                                } catch (NumberFormatException e) { }
                            }
                        }
                    }
                    lines.add(line);
                }
            } catch (IOException e) { }
        }

        if (updated) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException e) { }
            if (statusLabel != null) statusLabel.setText(String.format("Earned $%.2f!", earned));
        } else {
            if (statusLabel != null) statusLabel.setText("Goal not found.");
        }
    }

    @FXML
    private void handlePause() {
        isManuallyPaused = !isManuallyPaused;
        if (pauseButton != null) {
            if (isManuallyPaused) {
                pauseButton.setText("Resume");
                if (statusLabel != null) statusLabel.setText("Paused Manually");
            } else {
                pauseButton.setText("Pause");
                if (statusLabel != null) statusLabel.setText("Studying...");
            }
        }
    }

    @FXML
    private void handleCancel() throws IOException {
        if (timeline != null) timeline.stop();
        if (studyDriver != null) {
            try { studyDriver.quit(); } catch (Exception e) {}
        }
        App.setRoot("startStudying");
    }
}