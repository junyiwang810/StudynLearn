package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class SettingsController {

    @FXML
    private TextField hourlyRateField;

    @FXML
    private TextField balanceField;

    @FXML
    private ListView<String> blockedSitesListView;

    @FXML
    private TextField newBlockedSiteField;

    private static final String SETTINGS_FILE = "settings.properties";

    @FXML
    public void initialize() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(SETTINGS_FILE)) {
            props.load(in);
            hourlyRateField.setText(props.getProperty("hourlyRate", ""));
            balanceField.setText(props.getProperty("balance", ""));
        } catch (IOException e) {
            // File might not exist yet, ignore
        }
        loadBlacklist();
    }

    @FXML
    private void addBlockedSite() {
        String site = newBlockedSiteField.getText().trim();
        if (!site.isEmpty() && !blockedSitesListView.getItems().contains(site)) {
            blockedSitesListView.getItems().add(site);
            newBlockedSiteField.clear();
            saveBlacklist();
        }
    }

    @FXML
    private void removeBlockedSite() {
        String selected = blockedSitesListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            blockedSitesListView.getItems().remove(selected);
            saveBlacklist();
        }
    }

    private void loadBlacklist() {
        File file = new File("blacklist.txt");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    blockedSitesListView.getItems().add(line);
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void saveBlacklist() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("blacklist.txt"))) {
            for (String site : blockedSitesListView.getItems()) {
                writer.write(site);
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("hourlyRate", hourlyRateField.getText());
        props.setProperty("balance", balanceField.getText());

        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "User Settings");
            showAlert(AlertType.INFORMATION, "Success", "Settings saved successfully!");
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Error", "Could not save settings.");
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