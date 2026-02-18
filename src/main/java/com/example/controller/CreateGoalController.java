package com.example.controller;

import com.example.App;
import com.example.model.Goal;
import com.example.model.ProductDetails;
import com.example.repository.GoalRepository;
import com.example.service.ProductScraperService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.Optional;

public class CreateGoalController {

    @FXML private TextField linkTextField;
    @FXML private TextField nameTextField;
    @FXML private TextField costTextField;

    private final GoalRepository goalRepository = new GoalRepository();
    private final ProductScraperService scraperService = new ProductScraperService();

    @FXML
    private void addGoal() {
        String link = linkTextField.getText().trim();
        String name = nameTextField.getText().trim();
        String costText = costTextField.getText().trim();

        if (!link.isEmpty() && (name.isEmpty() || costText.isEmpty())) {
            fetchProductDetails(link);
            return;
        }

        if (name.isEmpty() || costText.isEmpty()) {
            showAlert(
                Alert.AlertType.WARNING,
                "Missing Information",
                "Provide a link to fetch details, or enter a name and cost manually."
            );
            return;
        }

        double cost;
        try {
            cost = Double.parseDouble(costText);
        } catch (NumberFormatException ignored) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number for the cost.");
            return;
        }

        try {
            goalRepository.appendGoal(new Goal(name, cost, 0.0));
            showAlert(Alert.AlertType.INFORMATION, "Success", "Goal '" + name + "' was added successfully.");
            nameTextField.clear();
            costTextField.clear();
            linkTextField.clear();
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
                "Could not fetch product details. Verify link and ChromeDriver setup."
            );
            return;
        }

        ProductDetails product = details.get();
        nameTextField.setText(product.getName());
        costTextField.setText(String.valueOf(product.getPrice()));
        showAlert(
            Alert.AlertType.INFORMATION,
            "Details Fetched",
            "Product details were filled in. Verify and click Add Goal again."
        );
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
