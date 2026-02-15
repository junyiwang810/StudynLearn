package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

public class StartStudyingController {

    @FXML
    private Label goalNameLabel;

    @FXML
    private Label resultLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TextField studyHoursField;

    private double hourlyRate = 0;
    private Goal currentGoal;

    @FXML
    public void initialize() {
        loadSettings();
        loadCurrentGoal();
    }

    private void loadSettings() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
            try {
                hourlyRate = Double.parseDouble(props.getProperty("hourlyRate", "0"));
            } catch (NumberFormatException e) {
                // Handle invalid numbers gracefully
            }
        } catch (IOException e) {
            // Settings file might not exist yet
        }
    }

    private void loadCurrentGoal() {
        String mainGoalName = "";
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
            mainGoalName = props.getProperty("currentGoalName", "");
        } catch (IOException e) { }

        if (mainGoalName.isEmpty()) {
            goalNameLabel.setText("No Main Goal Set");
            resultLabel.setText("Please go to Goals and set a main goal.");
            studyHoursField.setDisable(true);
            return;
        }

        File file = new File("goals.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Parse Name,Cost,Progress
                    int lastComma = line.lastIndexOf(',');
                    if (lastComma != -1) {
                        String partBeforeLast = line.substring(0, lastComma);
                        String lastPart = line.substring(lastComma + 1);
                        
                        String name = partBeforeLast;
                        String costStr = lastPart;
                        double currentAmount = 0.0;

                        // Check for 3rd column (Progress)
                        int secondLastComma = partBeforeLast.lastIndexOf(',');
                        if (secondLastComma != -1 && isNumeric(partBeforeLast.substring(secondLastComma + 1))) {
                            name = partBeforeLast.substring(0, secondLastComma);
                            costStr = partBeforeLast.substring(secondLastComma + 1);
                            currentAmount = Double.parseDouble(lastPart);
                        }

                        try {
                            double cost = Double.parseDouble(costStr);
                            if (name.equals(mainGoalName)) {
                                currentGoal = new Goal(name, cost, currentAmount);
                                goalNameLabel.setText("Working towards: " + name);
                                calculateHours(currentGoal);
                                return;
                            }
                        } catch (NumberFormatException e) {
                            // ignore invalid lines
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        goalNameLabel.setText("Goal Not Found");
        resultLabel.setText("The selected main goal could not be found.");
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void calculateHours(Goal goal) {
        if (goal == null) return;

        if (hourlyRate <= 0) {
            resultLabel.setText("Please set a valid hourly rate in Settings to calculate study time.");
            progressBar.setProgress(0.0);
            progressBar.setTooltip(null);
            return;
        }

        // Calculate progress based on time needed (hours)
        double totalHoursNeeded = goal.cost / hourlyRate;
        double hoursDone = goal.currentAmount / hourlyRate;

        if (totalHoursNeeded > 0) {
            progressBar.setProgress(Math.min(hoursDone / totalHoursNeeded, 1.0));
        } else {
            progressBar.setProgress(1.0);
        }

        // Show exact hours in a tooltip
        progressBar.setTooltip(new Tooltip(String.format("Studied: %.2f hrs / Total: %.2f hrs", hoursDone, totalHoursNeeded)));

        if (goal.currentAmount >= goal.cost) {
            resultLabel.setText(String.format("Goal Achieved! (%.2f / %.2f hours)\nYou have studied enough to afford %s.", hoursDone, totalHoursNeeded, goal.name));
        } else {
            double remainingHours = totalHoursNeeded - hoursDone;
            resultLabel.setText(String.format("Progress: %.2f / %.2f hours.\nStudy %.2f more hours to afford %s.", hoursDone, totalHoursNeeded, remainingHours, goal.name));
        }
    }

    @FXML
    private void handleStudySession() {
        try {
            double hours = Double.parseDouble(studyHoursField.getText());
            if (hours <= 0) {
                showAlert(AlertType.WARNING, "Invalid Input", "Please enter a positive number of hours.");
                return;
            }
            
            if (hourlyRate <= 0) {
                 showAlert(AlertType.WARNING, "Rate Error", "Please set an hourly rate in Settings first.");
                 return;
            }

            // Set session data and switch to timer view
            SessionData.hoursToStudy = hours;
            SessionData.hourlyRate = hourlyRate;
            SessionData.currentGoal = currentGoal;
            
            App.setRoot("timer");

        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Invalid Input", "Please enter a valid number for hours.");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Could not load the timer view.");
        }
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}