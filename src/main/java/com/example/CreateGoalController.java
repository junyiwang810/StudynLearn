package com.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class CreateGoalController {

    @FXML
    private TextField linkTextField;

    @FXML
    private TextField nameTextField;

    @FXML
    private TextField costTextField;

    @FXML
    private void addGoal() {
        String link = linkTextField.getText().trim();
        String name = nameTextField.getText().trim();
        String costStr = costTextField.getText().trim();

        // If a link is provided and other fields are empty, prioritize scraping.
        if (!link.isEmpty() && (name.isEmpty() || costStr.isEmpty())) {
            fetchProductDetailsFromLink(link);
            return; // End this action; user will click "Add Goal" again to save.
        }

        // If name and cost are present, add the goal.
        if (!name.isEmpty() && !costStr.isEmpty()) {
            try {
                double cost = Double.parseDouble(costStr);
                
                // Save the goal to a file (append mode)
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("goals.txt", true))) {
                    writer.write(name + "," + cost + ",0.0");
                    writer.newLine();
                }

                showAlert(AlertType.INFORMATION, "Success", "Goal '" + name + "' was added successfully!");

                nameTextField.clear();
                costTextField.clear();
                linkTextField.clear();
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Invalid Input", "Please enter a valid number for the cost.");
            } catch (IOException e) {
                showAlert(AlertType.ERROR, "Save Error", "Could not save the goal to file.");
            }
        } else {
            showAlert(AlertType.WARNING, "Missing Information", "Please provide a link to fetch details, or manually enter a name and a cost.");
        }
    }

    private void fetchProductDetailsFromLink(String link) {
        // IMPORTANT: You must download the ChromeDriver that matches your Chrome browser version.
        // Download from: https://googlechromelabs.github.io/chrome-for-testing/
        // Then, update the path below to point to where you extracted 'chromedriver.exe'.
        try {
            String driverPath = "C:\\chromedriver-win64\\chromedriver.exe";
            
            // Check if the file actually exists to prevent crashes
            if (!new File(driverPath).exists()) {
                showAlert(AlertType.ERROR, "Setup Error", "ChromeDriver not found at:\n" + driverPath + "\n\nPlease check if the file is in a subfolder (e.g., chromedriver-win64) or move it to C:/webdrivers/.");
                return;
            }

            // IMPORTANT: You must change this path to where you saved chromedriver.exe.
            // Use forward slashes (/) for the path, as shown in the example.
            System.setProperty("webdriver.chrome.driver", driverPath);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // Runs Chrome in the background without a UI.
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            WebDriver driver = new ChromeDriver(options);

            driver.get(link);

            // These selectors are examples for Amazon.com and will fail on other websites.
            String productName = driver.findElement(By.id("productTitle")).getText();
            String productPrice = driver.findElement(By.cssSelector(".a-price-whole")).getText().replace(".", "") + "." +
                                  driver.findElement(By.cssSelector(".a-price-fraction")).getText();

            driver.quit();

            nameTextField.setText(productName);
            costTextField.setText(productPrice.replaceAll("[^\\d.]", "")); // Clean up price string
            showAlert(AlertType.INFORMATION, "Details Fetched", "Product details have been filled. Please verify and click 'Add Goal' again to save.");

        } catch (NoSuchElementException e) {
            showAlert(AlertType.ERROR, "Scraping Failed", "Could not find the product details on the page. This can happen if the website is not Amazon or has changed its layout. Please enter the details manually.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Scraping Error", "An error occurred. Please ensure the link is correct and your ChromeDriver is configured properly.\nDetails: " + e.getMessage());
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