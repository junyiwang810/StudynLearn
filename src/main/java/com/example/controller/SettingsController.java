package com.example.controller;

import com.example.App;
import com.example.repository.BlacklistRepository;
import com.example.repository.SettingsRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SettingsController {

    @FXML private TextField hourlyRateField;
    @FXML private TextField balanceField;
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
    private void saveSettings() {
        Properties props = settingsRepository.load();
        props.setProperty("hourlyRate", hourlyRateField == null ? "" : hourlyRateField.getText().trim());
        props.setProperty("balance", balanceField == null ? "" : balanceField.getText().trim());

        try {
            settingsRepository.save(props, "User Settings");
            showAlert(Alert.AlertType.INFORMATION, "Success", "Settings saved successfully.");
        } catch (IOException ignored) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save settings.");
        }
    }

    @FXML
    private void saveAndBack() throws IOException {
        saveSettings();
        App.setRoot("primary");
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
        alert.showAndWait();
    }
}
