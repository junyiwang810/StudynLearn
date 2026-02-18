package com.example.controller;

import com.example.App;
import com.example.repository.BlacklistRepository;
import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;
import com.example.service.RewardService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TimerController {

    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Button pauseButton;
    @FXML private Button startSessionButton;

    private int remainingSeconds;
    private int studyDurationSeconds;
    private int breakDurationSeconds;
    private int totalSessions;
    private int currentSession = 1;

    private boolean manuallyPaused;
    private boolean browserBlocked;
    private boolean onBreak;

    private Timeline timeline;
    private WebDriver studyDriver;

    private final List<String> whitelist = new ArrayList<>();
    private List<String> blacklist = new ArrayList<>();

    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final BlacklistRepository blacklistRepository = new BlacklistRepository();
    private final RewardService rewardService = new RewardService(new GoalRepository(), settingsRepository);

    @FXML
    public void initialize() {
        loadSettings();
        blacklist = blacklistRepository.loadSites();

        onBreak = false;
        remainingSeconds = studyDurationSeconds;
        updateTimerLabel();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);

        hideStartSessionButton();
        startCurrentPhase();
    }

    private void loadSettings() {
        studyDurationSeconds = settingsRepository.getInt(
            "studyDurationSeconds",
            settingsRepository.getInt("studyDuration", 25) * 60
        );
        breakDurationSeconds = settingsRepository.getInt(
            "breakDurationSeconds",
            settingsRepository.getInt("breakDuration", 5) * 60
        );
        totalSessions = settingsRepository.getInt("sessions", 1);
    }

    private void startCurrentPhase() {
        manuallyPaused = false;
        browserBlocked = false;
        pauseButton.setText("Pause");
        pauseButton.setDisable(false);

        if (onBreak) {
            setStatus("Break time!");
        } else {
            setStatus("Studying (Session " + currentSession + ")...");
        }

        hideStartSessionButton();
        updateTimerLabel();
        timeline.play();
    }

    private void tick() {
        if (manuallyPaused) {
            return;
        }

        if (remainingSeconds > 0) {
            remainingSeconds--;
            updateTimerLabel();
            if (!onBreak) {
                checkBrowser();
            }
            return;
        }

        completeCurrentPhase();
    }

    private void completeCurrentPhase() {
        timeline.stop();

        if (!onBreak) {
            if (browserBlocked) {
                setStatus("Session failed: distraction detected.");
            } else {
                setStatus(rewardService.applyRewardForStudySession());
            }

            if (currentSession >= totalSessions) {
                finishAndReturnHome();
                return;
            }

            onBreak = true;
            remainingSeconds = breakDurationSeconds;
            updateTimerLabel();
            showStartSessionButton("Start Break");
            setStatus("Study finished. Click Start Break.");
            return;
        }

        onBreak = false;
        currentSession++;
        remainingSeconds = studyDurationSeconds;
        updateTimerLabel();
        showStartSessionButton("Start Session");
        setStatus("Break finished. Click Start Session.");
    }

    private void checkBrowser() {
        if (studyDriver == null) {
            return;
        }

        try {
            String currentUrl = studyDriver.getCurrentUrl();
            if (currentUrl == null) {
                return;
            }

            String lowerUrl = currentUrl.toLowerCase();
            for (String allowed : whitelist) {
                if (lowerUrl.contains(allowed.toLowerCase())) {
                    return;
                }
            }

            for (String blocked : blacklist) {
                if (lowerUrl.contains(blocked.toLowerCase())) {
                    browserBlocked = true;
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void updateTimerLabel() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    @FXML
    private void handlePause() {
        if (pauseButton.isDisable()) {
            return;
        }

        manuallyPaused = !manuallyPaused;
        pauseButton.setText(manuallyPaused ? "Resume" : "Pause");

        if (manuallyPaused) {
            setStatus("Paused manually");
        } else if (onBreak) {
            setStatus("Break time!");
        } else {
            setStatus("Studying (Session " + currentSession + ")...");
        }
    }

    @FXML
    private void handleStartSession() {
        startCurrentPhase();
    }

    @FXML
    private void handleCancel() throws IOException {
        if (timeline != null) {
            timeline.stop();
        }
        if (studyDriver != null) {
            try {
                studyDriver.quit();
            } catch (Exception ignored) {
            }
        }
        App.setRoot("startStudying");
    }

    private void showStartSessionButton(String text) {
        startSessionButton.setText(text);
        startSessionButton.setVisible(true);
        startSessionButton.setManaged(true);
    }

    private void hideStartSessionButton() {
        startSessionButton.setVisible(false);
        startSessionButton.setManaged(false);
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void finishAndReturnHome() {
        setStatus("All sessions complete!");
        pauseButton.setDisable(true);
        hideStartSessionButton();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Session Complete");
            alert.setHeaderText(null);
            alert.setContentText("Great work. Your session is complete.");
            var css = getClass().getResource("/com/example/styles.css");
            if (css != null) {
                alert.getDialogPane().getStylesheets().add(css.toExternalForm());
            }

            alert.setOnHidden(event -> {
                try {
                    App.setRoot("primary");
                } catch (IOException ignored) {
                    setStatus("Could not return to home page.");
                }
            });
            alert.show();
        });
    }
}
