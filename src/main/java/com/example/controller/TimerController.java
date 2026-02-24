package com.example.controller;

import com.example.App;
import com.example.repository.BlacklistRepository;
import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;
import com.example.model.Goal;
import com.example.service.RewardResult;
import com.example.service.RewardService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.openqa.selenium.WebDriver;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TimerController {
    @FXML private BorderPane timerRoot;
    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Label instructionLabel;
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
        Platform.runLater(() -> {
            if (timerRoot != null) {
                timerRoot.requestFocus();
            }
        });
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
            setBreakStatus();
        } else {
            setFocusStatus();
        }

        hideStartSessionButton();
        updateTimerLabel();
        timeline.play();
    }

    private void tick() {
        try {
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
        } catch (Throwable e) {
            if (timeline != null) {
                timeline.stop();
            }
            setStatus("Timer error");
            setInstruction("A runtime error occurred. Return and restart the session.");
        }
    }

    private void completeCurrentPhase() {
        timeline.stop();

        if (!onBreak) {
            if (browserBlocked) {
                setStatus("Focus session interrupted.");
                setInstruction("Distraction detected. Reward was not applied for this session.");
                advanceAfterStudyPhase();
                return;
            }

            RewardResult rewardResult = rewardService.applyRewardForStudySession();
            setStatus("Focus session complete.");
            setInstruction(rewardResult.getMessage());

            Platform.runLater(() -> {
                boolean redirected = rewardResult.isSuccess() && handleGoalCompletionAndOverflow(rewardResult);
                if (!redirected) {
                    advanceAfterStudyPhase();
                }
            });
            return;
        }

        onBreak = false;
        currentSession++;
        remainingSeconds = studyDurationSeconds;
        updateTimerLabel();
        showStartSessionButton("Start Session");
        setStatus("Break complete.");
        setInstruction("Click Start Session to continue.");
    }

    private void advanceAfterStudyPhase() {
        if (currentSession >= totalSessions) {
            finishAndReturnHome();
            return;
        }

        onBreak = true;
        remainingSeconds = breakDurationSeconds;
        updateTimerLabel();
        showStartSessionButton("Start Break");
        setStatus("Focus session complete.");
        setInstruction("Click Start Break to continue.");
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
            setStatus("Timer paused.");
            setInstruction("Press Resume when you are ready.");
        } else if (onBreak) {
            setBreakStatus();
        } else {
            setFocusStatus();
        }
    }

    @FXML
    private void handleStartSession() {
        startCurrentPhase();
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (event.getCode() != KeyCode.K) {
            return;
        }
        if (remainingSeconds > 2) {
            remainingSeconds = 2;
            updateTimerLabel();
        }
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

    private void setInstruction(String message) {
        if (instructionLabel != null) {
            instructionLabel.setText(message);
        }
    }

    private void setFocusStatus() {
        setStatus("Focus Session " + currentSession + " of " + totalSessions);
        setInstruction("Stay on task.");
    }

    private void setBreakStatus() {
        setStatus("Break " + currentSession + " of " + totalSessions);
        setInstruction("Take a short break.");
    }

    private void finishAndReturnHome() {
        setStatus("All sessions complete!");
        setInstruction("Great work. Returning to home page.");
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

    private boolean handleGoalCompletionAndOverflow(RewardResult result) {
        if (result.isGoalCompleted() || isGoalCompleteNow(result.getMainGoalName())) {
            Alert completeInfo = new Alert(Alert.AlertType.INFORMATION);
            completeInfo.setTitle("Goal Complete");
            completeInfo.setHeaderText(null);
            completeInfo.setContentText("You reached 100% for " + result.getMainGoalName() + ".");
            completeInfo.showAndWait();

            if (!result.getPurchaseLink().isEmpty()) {
                Alert buyPrompt = new Alert(Alert.AlertType.CONFIRMATION);
                buyPrompt.setTitle("Buy Item");
                buyPrompt.setHeaderText(null);
                buyPrompt.setContentText("Open the purchase link now?");
                if (buyPrompt.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    openLink(result.getPurchaseLink());
                }
            }

            askToSetAnotherMainGoal(result.getMainGoalName());
        }

        if (result.getOverflowAmount() > 0.0) {
            return handleOverflow(result.getMainGoalName(), result.getOverflowAmount());
        }
        return false;
    }

    private boolean isGoalCompleteNow(String goalName) {
        if (goalName == null || goalName.isEmpty()) {
            return false;
        }
        Optional<Goal> goal = new GoalRepository().findByName(goalName);
        return goal.isPresent() && goal.get().getCost() > 0 && goal.get().getCurrentAmount() >= goal.get().getCost();
    }

    private boolean handleOverflow(String mainGoalName, double overflowAmount) {
        ButtonType addToGoalBtn = new ButtonType("Add to Another Goal");
        ButtonType createGoalBtn = new ButtonType("Create New Goal");
        ButtonType cancelBtn = ButtonType.CANCEL;

        Alert optionPrompt = new Alert(Alert.AlertType.CONFIRMATION);
        optionPrompt.setTitle("Overflow Available");
        optionPrompt.setHeaderText(String.format("$%.2f overflow is available.", overflowAmount));
        optionPrompt.setContentText("Choose where to send the overflow.");
        optionPrompt.getButtonTypes().setAll(addToGoalBtn, createGoalBtn, cancelBtn);

        ButtonType selectedOption = optionPrompt.showAndWait().orElse(cancelBtn);
        if (selectedOption == cancelBtn) {
            return false;
        }

        if (selectedOption == createGoalBtn) {
            return queueOverflowForNewGoal(mainGoalName, overflowAmount);
        }

        List<Goal> allGoals = new GoalRepository().loadGoals();
        List<Goal> eligibleGoals = new ArrayList<>();
        for (Goal goal : allGoals) {
            if (goal.getName().equals(mainGoalName)) {
                continue;
            }
            boolean isComplete = goal.getCost() > 0 && goal.getCurrentAmount() >= goal.getCost();
            if (!isComplete) {
                eligibleGoals.add(goal);
            }
        }

        if (eligibleGoals.isEmpty()) {
            Alert createPrompt = new Alert(Alert.AlertType.CONFIRMATION);
            createPrompt.setTitle("No Eligible Goals");
            createPrompt.setHeaderText("No other goal can receive overflow right now.");
            createPrompt.setContentText("Do you want to create a new goal instead?");
            if (createPrompt.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                return queueOverflowForNewGoal(mainGoalName, overflowAmount);
            }
            return false;
        }

        List<String> names = new ArrayList<>();
        for (Goal goal : eligibleGoals) {
            names.add(goal.getName());
        }
        ChoiceDialog<String> choiceDialog = new ChoiceDialog<>(names.get(0), names);
        choiceDialog.setTitle("Select Goal");
        choiceDialog.setHeaderText(String.format("Move $%.2f overflow to which goal?", overflowAmount));
        choiceDialog.setContentText("Goal:");

        Optional<String> selectedGoal = choiceDialog.showAndWait();
        if (!selectedGoal.isPresent()) {
            return false;
        }

        try {
            GoalRepository repo = new GoalRepository();
            double remainingOverflow = repo.addAmountToGoalWithCap(selectedGoal.get(), overflowAmount);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Overflow Moved");
            success.setHeaderText(null);
            if (remainingOverflow > 0.0) {
                success.setContentText(String.format(
                    "Added as much as possible to %s. $%.2f is still leftover.",
                    selectedGoal.get(),
                    remainingOverflow
                ));
            } else {
                success.setContentText(String.format("Added $%.2f to %s.", overflowAmount, selectedGoal.get()));
            }
            success.showAndWait();

            if (remainingOverflow > 0.0) {
                Alert createPrompt = new Alert(Alert.AlertType.CONFIRMATION);
                createPrompt.setTitle("Overflow Remaining");
                createPrompt.setHeaderText(String.format("$%.2f is still remaining.", remainingOverflow));
                createPrompt.setContentText("Do you want to create a new goal for the remaining overflow?");
                if (createPrompt.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    return queueOverflowForNewGoal(mainGoalName, remainingOverflow);
                }
            }
        } catch (IOException ignored) {
            Alert fail = new Alert(Alert.AlertType.ERROR);
            fail.setTitle("Update Failed");
            fail.setHeaderText(null);
            fail.setContentText("Could not move overflow to the selected goal.");
            fail.showAndWait();
        }
        return false;
    }

    private boolean queueOverflowForNewGoal(String mainGoalName, double overflowAmount) {
        try {
            settingsRepository.setProperty("pendingOverflowAmount", String.valueOf(overflowAmount), "Overflow transfer");
            settingsRepository.setProperty("pendingOverflowFromGoal", mainGoalName, "Overflow transfer");
            App.setRoot("createGoal");
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void openLink(String link) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(link));
            }
        } catch (Throwable ignored) {
        }
    }

    private void askToSetAnotherMainGoal(String completedGoalName) {
        List<Goal> allGoals = new GoalRepository().loadGoals();
        List<String> candidateNames = new ArrayList<>();
        for (Goal goal : allGoals) {
            if (goal.getName().equals(completedGoalName)) {
                continue;
            }
            boolean completed = goal.getCost() > 0 && goal.getCurrentAmount() >= goal.getCost();
            if (!completed) {
                candidateNames.add(goal.getName());
            }
        }

        if (candidateNames.isEmpty()) {
            return;
        }

        Alert prompt = new Alert(Alert.AlertType.CONFIRMATION);
        prompt.setTitle("Set Main Goal");
        prompt.setHeaderText("Your current main goal is complete.");
        prompt.setContentText("Do you want to set another goal as main?");
        if (prompt.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        ChoiceDialog<String> choiceDialog = new ChoiceDialog<>(candidateNames.get(0), candidateNames);
        choiceDialog.setTitle("Choose Main Goal");
        choiceDialog.setHeaderText("Select your new main goal.");
        choiceDialog.setContentText("Goal:");
        Optional<String> selected = choiceDialog.showAndWait();
        if (!selected.isPresent()) {
            return;
        }

        try {
            settingsRepository.setProperty("currentGoalName", selected.get(), "Updated Main Goal");
        } catch (IOException ignored) {
        }
    }
}
