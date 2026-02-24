package com.example.service;

import com.example.model.ProductDetails;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductScraperService {
    public Optional<ProductDetails> fetchAmazonProduct(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(12));
            driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
            driver.get(url);

            String productName = firstNonEmptyText(driver, "#productTitle");
            String priceText = firstPrice(driver);
            double price = parsePrice(priceText);
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

    private String firstPrice(WebDriver driver) {
        String[] selectors = {
            "#corePrice_feature_div .a-price .a-offscreen",
            "#apex_desktop .a-price .a-offscreen",
            "#priceblock_ourprice",
            "#priceblock_dealprice",
            ".a-price .a-offscreen"
        };
        for (String selector : selectors) {
            String text = firstNonEmptyText(driver, selector);
            if (!text.isEmpty()) {
                return text;
            }
        }

        String whole = firstNonEmptyText(driver, ".a-price-whole");
        String fraction = firstNonEmptyText(driver, ".a-price-fraction");
        if (!whole.isEmpty()) {
            return whole + (fraction.isEmpty() ? "" : "." + fraction);
        }

        throw new NoSuchElementException("Price not found");
    }

    private String firstNonEmptyText(WebDriver driver, String cssSelector) {
        List<WebElement> elements = driver.findElements(By.cssSelector(cssSelector));
        for (WebElement element : elements) {
            String text = element.getText();
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
        }
        return "";
    }

    private double parsePrice(String rawPrice) {
        String compact = rawPrice == null ? "" : rawPrice.replaceAll("\\s+", "");
        Matcher matcher = Pattern.compile("(\\d[\\d,]*\\.?\\d{0,2})").matcher(compact);
        if (!matcher.find()) {
            throw new NumberFormatException("No number in price");
        }
        String normalized = matcher.group(1).replace(",", "");
        if (!normalized.contains(".")) {
            normalized = normalized + ".00";
        }
        return Double.parseDouble(normalized);
    }
}
