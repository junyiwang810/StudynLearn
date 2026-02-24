package com.example.controller;

import com.example.App;
import com.example.repository.BlacklistRepository;
import com.example.repository.SettingsRepository;
import com.example.service.StudyRateService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SettingsController {

    @FXML private TextField hourlyRateField;
    @FXML private TextField balanceField;
    @FXML private Label studyRateLabel;
    @FXML private ListView<String> blockedSitesListView;
    @FXML private TextField newBlockedSiteField;

    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final BlacklistRepository blacklistRepository = new BlacklistRepository();

    @FXML
    public void initialize() {
        Properties props = settingsRepository.load();
        if (hourlyRateField != null) {
            hourlyRateField.setText(props.getProperty("hourlyRate", ""));
        }
        if (balanceField != null) {
            balanceField.setText(props.getProperty("balance", ""));
        }
        if (blockedSitesListView != null) {
            blockedSitesListView.getItems().setAll(blacklistRepository.loadSites());
        }
        bindRatePreview();
        updateRatePreview();
    }

    @FXML
    private void addBlockedSite() {
        if (blockedSitesListView == null || newBlockedSiteField == null) {
            return;
        }
        String site = newBlockedSiteField.getText().trim();
        if (site.isEmpty() || blockedSitesListView.getItems().contains(site)) {
            return;
        }
        blockedSitesListView.getItems().add(site);
        newBlockedSiteField.clear();
        saveBlacklist();
    }

    @FXML
    private void removeBlockedSite() {
        if (blockedSitesListView == null) {
            return;
        }
        String selected = blockedSitesListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        blockedSitesListView.getItems().remove(selected);
        saveBlacklist();
    }

    @FXML
    private boolean saveSettings() {
        Properties props = settingsRepository.load();
        String hourlyRateText = hourlyRateField == null ? "" : hourlyRateField.getText().trim();
        String balanceText = balanceField == null ? "" : balanceField.getText().trim();
        props.setProperty("hourlyRate", hourlyRateText);
        props.setProperty("balance", balanceText);
        props.setProperty("studyHourlyRate", String.valueOf(
            StudyRateService.calculate(parseDouble(hourlyRateText), parseDouble(balanceText))
        ));

        try {
            settingsRepository.save(props, "User Settings");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Settings saved successfully.");
            return true;
        } catch (IOException ignored) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save settings.");
            return false;
        }
    }

    @FXML
    private void saveAndBack() throws IOException {
        if (saveSettings()) {
            App.setRoot("primary");
        }
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    private void saveBlacklist() {
        if (blockedSitesListView == null) {
            return;
        }
        List<String> items = new ArrayList<>(blockedSitesListView.getItems());
        try {
            blacklistRepository.saveSites(items);
        } catch (IOException ignored) {
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        var css = getClass().getResource("/com/example/styles.css");
        if (css != null) {
            alert.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        alert.showAndWait();
    }

    private void bindRatePreview() {
        if (hourlyRateField != null) {
            hourlyRateField.textProperty().addListener((obs, oldValue, newValue) -> updateRatePreview());
        }
        if (balanceField != null) {
            balanceField.textProperty().addListener((obs, oldValue, newValue) -> updateRatePreview());
        }
    }

    private void updateRatePreview() {
        if (studyRateLabel == null) {
            return;
        }
        double hourly = parseDouble(hourlyRateField == null ? "" : hourlyRateField.getText());
        double balance = parseDouble(balanceField == null ? "" : balanceField.getText());
        double studyRate = StudyRateService.calculate(hourly, balance);
        studyRateLabel.setText(String.format("$%.2f / hr", studyRate));
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }
}
