package com.example.service;

import com.example.model.ProductDetails;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.Optional;

public class ProductScraperService {
    private static final String DRIVER_PATH = "C:\\chromedriver-win64\\chromedriver.exe";

    public Optional<ProductDetails> fetchAmazonProduct(String url) {
        if (!new File(DRIVER_PATH).exists()) {
            return Optional.empty();
        }

        System.setProperty("webdriver.chrome.driver", DRIVER_PATH);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.get(url);

            String productName = driver.findElement(By.id("productTitle")).getText().trim();
            String whole = driver.findElement(By.cssSelector(".a-price-whole")).getText().replace(".", "");
            String fraction = driver.findElement(By.cssSelector(".a-price-fraction")).getText();
            String priceText = (whole + "." + fraction).replaceAll("[^\\d.]", "");
            double price = Double.parseDouble(priceText);
            return Optional.of(new ProductDetails(productName, price));
        } catch (NoSuchElementException | NumberFormatException ignored) {
            return Optional.empty();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
