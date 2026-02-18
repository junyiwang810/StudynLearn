package com.example.controller;

import com.example.App;
import com.example.model.Goal;
import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;
import com.example.service.GoalProgressService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoalsController {

    @FXML private ListView<Goal> goalsListView;

    private final GoalRepository goalRepository = new GoalRepository();
    private final GoalProgressService goalProgressService = new GoalProgressService(
        goalRepository,
        new SettingsRepository()
    );
    private final List<Goal> goals = new ArrayList<>();
    private String mainGoalName = "";

    @FXML
    public void initialize() {
        mainGoalName = goalProgressService.getMainGoalName();
        goals.clear();
        goals.addAll(goalRepository.loadGoals());
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
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                String displayName = item.getName();
                if (displayName.length() > 25) {
                    displayName = displayName.substring(0, 22) + "...";
                }

                Label nameLabel = new Label(displayName);
                nameLabel.setStyle("-fx-text-fill: black; -fx-font-size: 14px;");

                Button deleteBtn = new Button("\uD83D\uDDD1");
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
                deleteBtn.setOnAction(e -> deleteGoal(item));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label priceLabel = new Label(String.format("%.2f", item.getCost()));
                priceLabel.setStyle("-fx-text-fill: black;");

                boolean isMainGoal = item.getName().equals(mainGoalName);
                Button starBtn = new Button("\u2605");
                starBtn.setStyle(
                    "-fx-background-color: transparent; -fx-font-size: 16px; -fx-text-fill: " +
                    (isMainGoal ? "#fbc02d" : "#cccccc") + ";"
                );
                starBtn.setOnAction(e -> setMainGoal(item));

                row.getChildren().addAll(nameLabel, deleteBtn, spacer, priceLabel, starBtn);
                setGraphic(row);
                setStyle("-fx-background-color: transparent;");
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
}
