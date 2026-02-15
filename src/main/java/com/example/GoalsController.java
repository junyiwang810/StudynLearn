package com.example;

import java.io.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Callback;

public class GoalsController {

    @FXML private ListView<Goal> goalsListView;
    private List<Goal> goalsList = new ArrayList<>();

    @FXML
    public void initialize() {
        loadGoals();
        
        // Custom Cell Factory for the UI
        goalsListView.setCellFactory(new Callback<ListView<Goal>, ListCell<Goal>>() {
            @Override
            public ListCell<Goal> call(ListView<Goal> param) {
                return new ListCell<Goal>() {
                    @Override
                    protected void updateItem(Goal item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle("-fx-background-color: transparent;");
                        } else {
                            HBox hbox = new HBox(10);
                            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            
                            String displayName = item.name;
                            if (displayName.length() > 25) {
                                displayName = displayName.substring(0, 22) + "...";
                            }
                            Label nameLabel = new Label(displayName);
                            nameLabel.setStyle("-fx-text-fill: black; -fx-font-size: 14px;");
                            
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);
                            
                            Button deleteBtn = new Button("🗑"); // Trash icon
                            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: black;");
                            deleteBtn.setOnAction(e -> deleteGoal(item));

                            Label priceLabel = new Label(String.format("%.2f", item.cost));
                            priceLabel.setStyle("-fx-text-fill: black;");
                            
                            Button starBtn = new Button("★");
                            starBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + (isMainGoal(item.name) ? "#fbc02d" : "#cccccc") + "; -fx-font-size: 16px;");
                            starBtn.setOnAction(e -> setMainGoal(item));

                            hbox.getChildren().addAll(nameLabel, deleteBtn, spacer, priceLabel, starBtn);
                            
                            setStyle("-fx-background-color: transparent;");
                            
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });
        loadMainGoal();
        goalsListView.getItems().addAll(goalsList);
    }

    private void loadGoals() {
        goalsList.clear();
        File file = new File("goals.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Parsing logic similar to previous, simplified for brevity
                    int lastComma = line.lastIndexOf(',');
                    if (lastComma != -1) {
                        String partBeforeLast = line.substring(0, lastComma);
                        String lastPart = line.substring(lastComma + 1);
                        String name = partBeforeLast;
                        double cost = 0;
                        double current = 0;
                        int secondLastComma = partBeforeLast.lastIndexOf(',');
                         if (secondLastComma != -1) {
                                try {
                                    cost = Double.parseDouble(partBeforeLast.substring(secondLastComma + 1));
                                    name = partBeforeLast.substring(0, secondLastComma);
                                    current = Double.parseDouble(lastPart);
                                } catch (Exception e) {}
                         }
                         goalsList.add(new Goal(name, cost, current));
                    }
                }
            } catch (IOException e) { }
        }
    }
    private String mainGoalName = "";

    private void loadMainGoal() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream in = new java.io.FileInputStream("settings.properties")) {
            props.load(in);
            mainGoalName = props.getProperty("currentGoalName", "");
        } catch (java.io.IOException e) {
            mainGoalName = "";
        }
    }

    private boolean isMainGoal(String name) {
        return name != null && name.equals(mainGoalName);
    }

    private void deleteGoal(Goal goal) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Goal");
        alert.setHeaderText("Are you sure you want to delete this goal?");
        alert.setContentText(goal.name);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            goalsList.remove(goal);
            saveGoalsToFile();
            goalsListView.getItems().setAll(goalsList);
        }
    }

    private void setMainGoal(Goal goal) {
        mainGoalName = goal.name;
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
        } catch (IOException e) { }
        props.setProperty("currentGoalName", goal.name);
        try (FileOutputStream out = new FileOutputStream("settings.properties")) {
            props.store(out, null);
        } catch (IOException e) { }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Main Goal Set");
        alert.setHeaderText(null);
        alert.setContentText("You have set '" + goal.name + "' as your main goal.");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        alert.showAndWait();

        goalsListView.refresh();
    }

    private void saveGoalsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("goals.txt"))) {
            for (Goal g : goalsList) {
                writer.write(g.name + "," + g.cost + "," + g.currentAmount);
                writer.newLine();
            }
        } catch (IOException e) { }
    }

    @FXML private void switchToPrimary() throws IOException { App.setRoot("primary"); }
    @FXML private void switchToCreateGoal() throws IOException { App.setRoot("createGoal"); }
}