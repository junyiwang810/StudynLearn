package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;

public class GoalsController {

    @FXML
    private ListView<String> goalsListView;

    private List<String> rawGoalLines = new ArrayList<>();

    @FXML
    public void initialize() {
        File file = new File("goals.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Try to parse Name,Cost,Progress
                    int lastComma = line.lastIndexOf(',');
                    if (lastComma != -1) {
                        String name = "";
                        String cost = "";
                        
                        // Check if we have the new 3-part format (Name,Cost,Progress)
                        // We check if the part before the last comma ends with a number (Cost)
                        String partBeforeLast = line.substring(0, lastComma);
                        int secondLastComma = partBeforeLast.lastIndexOf(',');
                        
                        if (secondLastComma != -1) {
                            String potentialCost = partBeforeLast.substring(secondLastComma + 1);
                            try {
                                Double.parseDouble(potentialCost);
                                // It is likely Name,Cost,Progress
                                name = partBeforeLast.substring(0, secondLastComma);
                                cost = potentialCost;
                            } catch (NumberFormatException e) {
                                // Fallback to old format: Name,Cost
                                name = partBeforeLast;
                                cost = line.substring(lastComma + 1);
                            }
                        } else {
                            // Fallback to old format: Name,Cost
                            name = partBeforeLast;
                            cost = line.substring(lastComma + 1);
                        }

                        if (name.length() > 40) {
                            name = name.substring(0, 37) + "...";
                        }

                        rawGoalLines.add(line);
                        goalsListView.getItems().add(name + " - $" + cost);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void deleteGoal() {
        int selectedIndex = goalsListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            goalsListView.getItems().remove(selectedIndex);
            rawGoalLines.remove(selectedIndex);
            saveGoalsToFile();
        }
    }

    @FXML
    private void setMainGoal() {
        int selectedIndex = goalsListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            String rawLine = rawGoalLines.get(selectedIndex);
            String name = "";
            
            // Parse name from rawLine
            int lastComma = rawLine.lastIndexOf(',');
            if (lastComma != -1) {
                String partBeforeLast = rawLine.substring(0, lastComma);
                int secondLastComma = partBeforeLast.lastIndexOf(',');
                if (secondLastComma != -1) {
                    try {
                        Double.parseDouble(partBeforeLast.substring(secondLastComma + 1));
                        name = partBeforeLast.substring(0, secondLastComma);
                    } catch (NumberFormatException e) {
                        name = partBeforeLast;
                    }
                } else {
                    name = partBeforeLast;
                }
            }

            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream("settings.properties")) {
                props.load(in);
            } catch (IOException e) { /* File might not exist */ }
            
            props.setProperty("currentGoalName", name);
            
            try (FileOutputStream out = new FileOutputStream("settings.properties")) {
                props.store(out, "User Settings");
                showAlert(AlertType.INFORMATION, "Success", "Set '" + name + "' as main goal.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveGoalsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("goals.txt"))) {
            for (String line : rawGoalLines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }
}