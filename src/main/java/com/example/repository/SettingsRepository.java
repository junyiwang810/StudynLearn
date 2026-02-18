package com.example.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class SettingsRepository {
    private static final Path SETTINGS_PATH = Paths.get("settings.properties");

    public Properties load() {
        Properties properties = new Properties();
        if (!Files.exists(SETTINGS_PATH)) {
            return properties;
        }

        try (InputStream in = Files.newInputStream(SETTINGS_PATH)) {
            properties.load(in);
        } catch (IOException ignored) {
        }
        return properties;
    }

    public void save(Properties properties, String comment) throws IOException {
        try (OutputStream out = Files.newOutputStream(SETTINGS_PATH)) {
            properties.store(out, comment);
        }
    }

    public String getString(String key, String defaultValue) {
        return load().getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = load().getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String value = load().getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public void setProperty(String key, String value, String comment) throws IOException {
        Properties properties = load();
        properties.setProperty(key, value);
        save(properties, comment);
    }
}
