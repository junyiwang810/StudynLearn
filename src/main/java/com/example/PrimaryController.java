package com.example;

import java.io.*;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class PrimaryController {

    @FXML private Label percentageLabel;
    @FXML private Label fractionLabel;
    @FXML private Label goalNameLabel;

    private String mainGoalName = "";

    @FXML
    public void initialize() {
        loadMainGoal();
        updateProgress();
    }

    @FXML
    private void switchToStudying() throws IOException {
        App.setRoot("startStudying");
    }

    @FXML
    private void switchToSettings() throws IOException {
        App.setRoot("goals");
    }

    private void loadMainGoal() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
            mainGoalName = props.getProperty("currentGoalName", "");
        } catch (IOException e) {
            mainGoalName = "";
        }
    }

    private void updateProgress() {
        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader("goals.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int lastComma = line.lastIndexOf(',');
                if (lastComma != -1) {
                    String beforeLast = line.substring(0, lastComma);
                    int secondLastComma = beforeLast.lastIndexOf(',');
                    if (secondLastComma != -1) {
                        String name = beforeLast.substring(0, secondLastComma);
                        if (name.equals(mainGoalName)) {
                            double cost = Double.parseDouble(beforeLast.substring(secondLastComma + 1));
                            double current = Double.parseDouble(line.substring(lastComma + 1));
                            
                            if (cost > 0) {
                                int percent = (int)((current / cost) * 100);
                                percentageLabel.setText(percent + "% There!");
                                fractionLabel.setText(String.format("$%.2f / $%.2f", current, cost));
                            }
                            found = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) { }

        if (goalNameLabel != null) goalNameLabel.setText(mainGoalName.isEmpty() ? "No Goal Set" : mainGoalName);

        if (!found) {
            percentageLabel.setText("0% There!");
            fractionLabel.setText("$0.00 / $0.00");
        }
    }
}