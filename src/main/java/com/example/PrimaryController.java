package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class PrimaryController {

    @FXML
    private Label mainGoalLabel;

    @FXML
    private ProgressBar mainGoalProgressBar;

    public void initialize() {
        String mainGoalName = "";
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
            mainGoalName = props.getProperty("currentGoalName", "");
        } catch (IOException e) { }

        if (mainGoalName.isEmpty()) {
            return;
        }

        File file = new File("goals.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int lastComma = line.lastIndexOf(',');
                    if (lastComma != -1) {
                        String partBeforeLast = line.substring(0, lastComma);
                        String lastPart = line.substring(lastComma + 1);
                        
                        String name = partBeforeLast;
                        double cost = 0;
                        double progress = 0;

                        int secondLastComma = partBeforeLast.lastIndexOf(',');
                        if (secondLastComma != -1) {
                             try {
                                 cost = Double.parseDouble(partBeforeLast.substring(secondLastComma + 1));
                                 name = partBeforeLast.substring(0, secondLastComma);
                                 progress = Double.parseDouble(lastPart);
                             } catch (NumberFormatException e) { /* Ignore */ }
                        }

                        if (name.equals(mainGoalName) && cost > 0) {
                            mainGoalLabel.setText("Current Goal: " + name);
                            mainGoalProgressBar.setProgress(progress / cost);
                            return;
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void switchToGoals() throws IOException {
        App.setRoot("goals");
    }

    @FXML
    private void switchToCreateGoal() throws IOException {
        App.setRoot("createGoal");
    }

    @FXML
    private void switchToStartStudying() throws IOException {
        App.setRoot("startStudying");
    }

    @FXML
    private void switchToSettings() throws IOException {
        App.setRoot("settings");
    }
}
