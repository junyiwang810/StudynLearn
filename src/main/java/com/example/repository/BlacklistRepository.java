package com.example.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class BlacklistRepository {
    private static final Path BLACKLIST_PATH = Paths.get("blacklist.txt");

    public List<String> loadSites() {
        if (!Files.exists(BLACKLIST_PATH)) {
            return new ArrayList<>();
        }
        try {
            return Files.readAllLines(BLACKLIST_PATH);
        } catch (IOException ignored) {
            return new ArrayList<>();
        }
    }

    public void saveSites(List<String> sites) throws IOException {
        Files.write(BLACKLIST_PATH, sites, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
