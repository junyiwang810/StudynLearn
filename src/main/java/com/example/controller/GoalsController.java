package com.example.controller;

import com.example.App;
import com.example.model.Goal;
import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;
import com.example.service.GoalProgressService;
import com.example.service.RewardService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GoalsController {

    @FXML private ListView<Goal> goalsListView;

    private final GoalRepository goalRepository = new GoalRepository();
    private final GoalProgressService goalProgressService = new GoalProgressService(
        goalRepository,
        new SettingsRepository()
    );
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final List<Goal> goals = new ArrayList<>();
    private String mainGoalName = "";

    @FXML
    public void initialize() {
        mainGoalName = goalProgressService.getMainGoalName();
        goals.clear();
        goals.addAll(goalRepository.loadGoals());
        goalsListView.setPlaceholder(new Label("No goals yet. Add your first goal."));
        goalsListView.setCellFactory(param -> createCell());
        goalsListView.getItems().setAll(goals);
    }

    private ListCell<Goal> createCell() {
        return new ListCell<Goal>() {
            @Override
            protected void updateItem(Goal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                HBox row = new HBox(8);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.getStyleClass().add("goal-row");
                boolean completed = item.getCost() > 0 && item.getCurrentAmount() >= item.getCost();
                if (completed) {
                    row.getStyleClass().add("completed-goal-row");
                }

                String displayName = item.getName();
                if (displayName.length() > 25) {
                    displayName = displayName.substring(0, 22) + "...";
                }

                Label nameLabel = new Label(displayName);
                nameLabel.setStyle(
                    completed
                        ? "-fx-text-fill: #9ca3af; -fx-font-size: 14px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #111827; -fx-font-size: 14px; -fx-font-weight: bold;"
                );

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label priceLabel = new Label(String.format("$%.2f / $%.2f", item.getCurrentAmount(), item.getCost()));
                priceLabel.setStyle(completed ? "-fx-text-fill: #9ca3af; -fx-font-size: 13px;" : "-fx-text-fill: #374151; -fx-font-size: 13px;");

                boolean isMainGoal = item.getName().equals(mainGoalName);
                Button setMainButton = new Button(isMainGoal ? "\u2605 Main Goal" : "\u2606 Set as Main");
                setMainButton.getStyleClass().add("goal-action-button");
                setMainButton.setVisible(!completed);
                setMainButton.setManaged(!completed);
                setMainButton.setTooltip(new Tooltip("Set as Main"));
                setMainButton.setOnAction(e -> setMainGoal(item));

                String link = settingsRepository.getString(RewardService.goalLinkKey(item.getName()), "");
                Button openLinkButton = new Button("Open Link");
                openLinkButton.getStyleClass().add("goal-action-button");
                openLinkButton.setVisible(completed);
                openLinkButton.setManaged(completed);
                openLinkButton.setDisable(link.isEmpty());
                openLinkButton.setTooltip(new Tooltip(link.isEmpty() ? "No link saved" : "Open saved purchase link"));
                openLinkButton.setOnAction(e -> openLink(link));

                Button deleteBtn = new Button("Delete");
                deleteBtn.getStyleClass().add("danger-button");
                deleteBtn.setTooltip(new Tooltip("Delete Goal"));
                deleteBtn.setOnAction(e -> deleteGoal(item));

                if (isMainGoal) {
                    row.getStyleClass().add("main-goal-row");
                }

                row.getChildren().addAll(nameLabel, spacer, priceLabel, openLinkButton, setMainButton, deleteBtn);
                setGraphic(row);
                setText(null);
            }
        };
    }

    private void deleteGoal(Goal goal) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Goal");
        alert.setHeaderText("Are you sure you want to delete this goal?");
        alert.setContentText(goal.getName());
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        goals.remove(goal);
        try {
            goalRepository.saveGoals(goals);
            goalsListView.getItems().setAll(goals);
        } catch (IOException ignored) {
        }
    }

    private void setMainGoal(Goal goal) {
        try {
            goalProgressService.setMainGoalName(goal.getName());
            mainGoalName = goal.getName();
        } catch (IOException ignored) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Main Goal Set");
        alert.setHeaderText(null);
        alert.setContentText("You set '" + goal.getName() + "' as your main goal.");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());
        alert.showAndWait();

        goalsListView.refresh();
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    @FXML
    private void switchToCreateGoal() throws IOException {
        App.setRoot("createGoal");
    }

    private void openLink(String link) {
        if (link == null || link.trim().isEmpty()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(link.trim()));
            }
        } catch (Exception ignored) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Open Link Failed");
            alert.setHeaderText(null);
            alert.setContentText("Could not open the saved link.");
            alert.showAndWait();
        }
    }
}
