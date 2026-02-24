package com.example.controller;

import com.example.App;
import com.example.model.Goal;
import com.example.model.ProductDetails;
import com.example.repository.GoalRepository;
import com.example.repository.SettingsRepository;
import com.example.service.ProductScraperService;
import com.example.service.RewardService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CreateGoalController {

    @FXML private TextField linkTextField;
    @FXML private TextField nameTextField;
    @FXML private TextField costTextField;
    @FXML private Button fetchButton;
    @FXML private Button saveButton;

    private final GoalRepository goalRepository = new GoalRepository();
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final ProductScraperService scraperService = new ProductScraperService();

    @FXML
    public void initialize() {
        if (linkTextField != null) {
            linkTextField.textProperty().addListener((obs, oldVal, newVal) -> updateButtonState());
        }
        if (nameTextField != null) {
            nameTextField.textProperty().addListener((obs, oldVal, newVal) -> updateButtonState());
        }
        if (costTextField != null) {
            costTextField.textProperty().addListener((obs, oldVal, newVal) -> updateButtonState());
        }
        updateButtonState();
    }

    @FXML
    private void fetchFromLink() {
        String link = getFieldText(linkTextField);
        if (link.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Link", "Paste a product link first.");
            return;
        }
        fetchProductDetails(link);
        updateButtonState();
    }

    @FXML
    private void saveGoal() {
        String name = nameTextField.getText().trim();
        String costText = costTextField.getText().trim();

        if (name.isEmpty() || costText.isEmpty()) {
            showAlert(
                Alert.AlertType.WARNING,
                "Missing Information",
                "Enter both goal name and price before saving."
            );
            return;
        }

        double cost;
        try {
            cost = Double.parseDouble(costText);
        } catch (NumberFormatException ignored) {
            showAlert(Alert.AlertType.ERROR, "Invalid Price", "Price must be a valid number, like 129.99.");
            return;
        }

        try {
            goalRepository.appendGoal(new Goal(name, cost, 0.0));
            if (!getFieldText(linkTextField).isEmpty()) {
                settingsRepository.setProperty(
                    RewardService.goalLinkKey(name),
                    getFieldText(linkTextField),
                    "Saved goal purchase link"
                );
            }
            boolean redirectedToCreate = applyPendingOverflowToNewGoal(name);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Goal '" + name + "' was added successfully.");
            if (!redirectedToCreate) {
                App.setRoot("goals");
            }
        } catch (IOException ignored) {
            showAlert(Alert.AlertType.ERROR, "Save Error", "Could not save the goal.");
        }
    }

    private void fetchProductDetails(String link) {
        Optional<ProductDetails> details = scraperService.fetchAmazonProduct(link);
        if (!details.isPresent()) {
            showAlert(
                Alert.AlertType.ERROR,
                "Scraping Failed",
                "Could not fetch product details. You can still enter name and price manually."
            );
            return;
        }

        ProductDetails product = details.get();
        nameTextField.setText(product.getName());
        costTextField.setText(String.valueOf(product.getPrice()));
        showAlert(
            Alert.AlertType.INFORMATION,
            "Details Fetched",
            "Product details were filled in. Verify them, then click Save Goal."
        );
    }

    @FXML
    private void switchToGoals() throws IOException {
        App.setRoot("goals");
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

    private void updateButtonState() {
        if (fetchButton != null) {
            fetchButton.setDisable(getFieldText(linkTextField).isEmpty());
        }
        if (saveButton != null) {
            saveButton.setDisable(!isValidGoalInput());
        }
    }

    private boolean isValidGoalInput() {
        String name = getFieldText(nameTextField);
        String costText = getFieldText(costTextField);
        if (name.isEmpty() || costText.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(costText);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String getFieldText(TextField field) {
        return field == null ? "" : field.getText().trim();
    }

    private boolean applyPendingOverflowToNewGoal(String newGoalName) throws IOException {
        double pendingOverflow = settingsRepository.getDouble("pendingOverflowAmount", 0.0);
        if (pendingOverflow <= 0) {
            return false;
        }

        double remainingOverflow = goalRepository.addAmountToGoalWithCap(newGoalName, pendingOverflow);
        double appliedAmount = pendingOverflow - Math.max(0.0, remainingOverflow);
        settingsRepository.setProperty("currentGoalName", newGoalName, "Set overflow goal as main");
        settingsRepository.setProperty("pendingOverflowAmount", String.valueOf(Math.max(0.0, remainingOverflow)), "Overflow transfer");
        settingsRepository.setProperty("pendingOverflowFromGoal", "", "Clear overflow transfer");

        if (appliedAmount > 0.0) {
            showAlert(
                Alert.AlertType.INFORMATION,
                "Overflow Added",
                String.format("Added $%.2f overflow to '%s'.", appliedAmount, newGoalName)
            );
        }

        if (remainingOverflow <= 0.0) {
            return false;
        }

        return continueOverflowPrompt(newGoalName, remainingOverflow);
    }

    private boolean continueOverflowPrompt(String sourceGoalName, double overflowAmount) throws IOException {
        ButtonType addToGoalBtn = new ButtonType("Add to Another Goal");
        ButtonType createGoalBtn = new ButtonType("Create New Goal");
        ButtonType keepBtn = new ButtonType("Keep for Later");

        Alert optionPrompt = new Alert(Alert.AlertType.CONFIRMATION);
        optionPrompt.setTitle("Overflow Remaining");
        optionPrompt.setHeaderText(String.format("$%.2f is still remaining.", overflowAmount));
        optionPrompt.setContentText("What do you want to do with the remaining overflow?");
        optionPrompt.getButtonTypes().setAll(addToGoalBtn, createGoalBtn, keepBtn);

        ButtonType selectedOption = optionPrompt.showAndWait().orElse(keepBtn);
        if (selectedOption == createGoalBtn) {
            settingsRepository.setProperty("pendingOverflowAmount", String.valueOf(overflowAmount), "Overflow transfer");
            settingsRepository.setProperty("pendingOverflowFromGoal", sourceGoalName, "Overflow transfer");
            App.setRoot("createGoal");
            return true;
        }

        if (selectedOption == keepBtn) {
            settingsRepository.setProperty("pendingOverflowAmount", String.valueOf(overflowAmount), "Overflow transfer");
            settingsRepository.setProperty("pendingOverflowFromGoal", sourceGoalName, "Overflow transfer");
            return false;
        }

        List<Goal> allGoals = goalRepository.loadGoals();
        List<String> eligibleGoalNames = new ArrayList<>();
        for (Goal goal : allGoals) {
            if (goal.getName().equals(sourceGoalName)) {
                continue;
            }
            boolean isComplete = goal.getCost() > 0 && goal.getCurrentAmount() >= goal.getCost();
            if (!isComplete) {
                eligibleGoalNames.add(goal.getName());
            }
        }

        if (eligibleGoalNames.isEmpty()) {
            Alert noGoalPrompt = new Alert(Alert.AlertType.CONFIRMATION);
            noGoalPrompt.setTitle("No Eligible Goals");
            noGoalPrompt.setHeaderText("No other goal can receive this overflow.");
            noGoalPrompt.setContentText("Create another goal now?");
            if (noGoalPrompt.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                settingsRepository.setProperty("pendingOverflowAmount", String.valueOf(overflowAmount), "Overflow transfer");
                settingsRepository.setProperty("pendingOverflowFromGoal", sourceGoalName, "Overflow transfer");
                App.setRoot("createGoal");
                return true;
            }
            settingsRepository.setProperty("pendingOverflowAmount", String.valueOf(overflowAmount), "Overflow transfer");
            settingsRepository.setProperty("pendingOverflowFromGoal", sourceGoalName, "Overflow transfer");
            return false;
        }

        ChoiceDialog<String> chooseGoal = new ChoiceDialog<>(eligibleGoalNames.get(0), eligibleGoalNames);
        chooseGoal.setTitle("Select Goal");
        chooseGoal.setHeaderText(String.format("Move $%.2f overflow to which goal?", overflowAmount));
        chooseGoal.setContentText("Goal:");
        Optional<String> selectedGoal = chooseGoal.showAndWait();
        if (!selectedGoal.isPresent()) {
            settingsRepository.setProperty("pendingOverflowAmount", String.valueOf(overflowAmount), "Overflow transfer");
            settingsRepository.setProperty("pendingOverflowFromGoal", sourceGoalName, "Overflow transfer");
            return false;
        }

        double newRemaining = goalRepository.addAmountToGoalWithCap(selectedGoal.get(), overflowAmount);
        if (newRemaining > 0.0) {
            return continueOverflowPrompt(selectedGoal.get(), newRemaining);
        }
        settingsRepository.setProperty("pendingOverflowAmount", "0", "Clear overflow transfer");
        settingsRepository.setProperty("pendingOverflowFromGoal", "", "Clear overflow transfer");
        return false;
    }
}
