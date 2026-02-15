package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.media.AudioClip;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimerController {

    @FXML
    private Label timerLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button breakButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button actionButton;

    private Timeline timeline;
    private long timeSeconds;
    private long totalTimeSeconds;
    private WebDriver studyDriver;
    private List<String> whitelist = new ArrayList<>();
    private List<String> blacklist = new ArrayList<>();
    private boolean isManuallyPaused = false;
    private boolean isOnBreak = false;
    private long breakTimeSeconds;

    @FXML
    public void initialize() {
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);
        
        loadBlacklist();
        whitelist.add("youtube.com/watch?v=dQw4w9WgXcQ");

        startSession();
    }

    private void startSession() {
        double hours = SessionData.hoursToStudy;
        if (hours <= 0) {
            statusLabel.setText("Invalid session data.");
            return;
        }

        // Connect to Chrome
        try {
            System.setProperty("webdriver.chrome.driver", "C:\\chromedriver-win64\\chromedriver.exe");
            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
            studyDriver = new ChromeDriver(options);
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Connection Error", "Could not connect to Chrome.\nPlease launch Chrome with:\nchrome.exe --remote-debugging-port=9222");
            try { switchToStartStudying(); } catch (IOException ex) {}
            return;
        }

        totalTimeSeconds = (long) (hours * 3600);
        timeSeconds = totalTimeSeconds;
        updateTimerLabel();
        progressBar.setProgress(0.0);
        statusLabel.setText("Studying...");

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (isOnBreak) {
                breakTimeSeconds--;
                statusLabel.setText(String.format("Break: %02d:%02d", breakTimeSeconds / 60, breakTimeSeconds % 60));
                if (breakTimeSeconds <= 0) {
                    handleBreak();
                    if (!isManuallyPaused) {
                        handlePause();
                    }
                    playSound();
                }
                return;
            }

            boolean browserBlocked = false;

            // Check browser
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
                                    statusLabel.setText("PAUSED: " + blocked + " detected!");
                                    browserBlocked = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!browserBlocked) {
                        if (isManuallyPaused) {
                            statusLabel.setText("Paused Manually");
                        } else {
                            statusLabel.setText("Studying...");
                        }
                    }
                } catch (Exception ex) { 
                    if (isManuallyPaused) {
                        statusLabel.setText("Paused Manually");
                    } else {
                        statusLabel.setText("Studying...");
                    }
                }
            }

            if (!browserBlocked && !isManuallyPaused) {
                timeSeconds--;
                updateTimerLabel();

                if (totalTimeSeconds > 0) {
                    double progress = (double) (totalTimeSeconds - timeSeconds) / totalTimeSeconds;
                    progressBar.setProgress(progress);
                }

                if (timeSeconds <= 0) {
                    timeline.stop();
                    Platform.runLater(this::completeSession);
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleBreak() {
        isOnBreak = !isOnBreak;
        if (isOnBreak) {
            breakTimeSeconds = 300;
            breakButton.setText("End Break");
            statusLabel.setText("On Break");
        } else {
            breakButton.setText("Take 5m Break");
            statusLabel.setText(isManuallyPaused ? "Paused Manually" : "Studying...");
        }
    }

    @FXML
    private void handlePause() {
        isManuallyPaused = !isManuallyPaused;
        if (isManuallyPaused) {
            pauseButton.setText("Resume");
            statusLabel.setText("Paused Manually");
        } else {
            pauseButton.setText("Pause");
            statusLabel.setText("Studying...");
        }
    }

    @FXML
    private void handleCancel() throws IOException {
        if (timeline != null) timeline.stop();
        if (studyDriver != null) {
            try { studyDriver.quit(); } catch (Exception e) {}
        }
        switchToStartStudying();
    }

    private void completeSession() {
        double earnings = SessionData.hoursToStudy * SessionData.hourlyRate;
        Goal goal = SessionData.currentGoal;

        if (goal != null) {
            goal.currentAmount += earnings;
            updateGoalInFile(goal);
        }

        if (studyDriver != null) {
            try { studyDriver.quit(); } catch (Exception e) {}
        }

        playSound();
        bringToFront();
        
        showAlert(AlertType.INFORMATION, "Session Complete", String.format("Finished %.2f hours!\nEarned $%.2f.", SessionData.hoursToStudy, earnings));
        try { switchToStartStudying(); } catch (IOException e) {}
    }

    private void updateTimerLabel() {
        long hours = timeSeconds / 3600;
        long minutes = (timeSeconds % 3600) / 60;
        long seconds = timeSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateGoalInFile(Goal goalToUpdate) {
        File file = new File("goals.txt");
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(goalToUpdate.name + "," + goalToUpdate.cost)) {
                    lines.add(goalToUpdate.name + "," + goalToUpdate.cost + "," + goalToUpdate.currentAmount);
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadBlacklist() {
        blacklist.clear();
        File file = new File("blacklist.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) blacklist.add(line.trim());
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void playSound() {
        try {
            java.net.URL resource = getClass().getResource("notification.wav");
            if (resource != null) new AudioClip(resource.toString()).play();
            else java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {}
    }

    private void bringToFront() {
        if (actionButton.getScene() != null && actionButton.getScene().getWindow() != null) {
            Stage stage = (Stage) actionButton.getScene().getWindow();
            if (stage.isIconified()) stage.setIconified(false);
            stage.setAlwaysOnTop(true);
            stage.setAlwaysOnTop(false);
            stage.toFront();
            stage.requestFocus();
        }
    }

    private void switchToStartStudying() throws IOException {
        App.setRoot("startStudying");
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}