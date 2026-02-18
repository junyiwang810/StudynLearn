package com.example.repository;

import com.example.model.Goal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GoalRepository {
    private static final Path GOALS_PATH = Paths.get("goals.txt");

    public List<Goal> loadGoals() {
        List<Goal> goals = new ArrayList<>();
        if (!Files.exists(GOALS_PATH)) {
            return goals;
        }

        try {
            List<String> lines = Files.readAllLines(GOALS_PATH);
            for (String line : lines) {
                parseGoal(line).ifPresent(goals::add);
            }
        } catch (IOException ignored) {
        }
        return goals;
    }

    public Optional<Goal> findByName(String name) {
        return loadGoals().stream().filter(goal -> goal.getName().equals(name)).findFirst();
    }

    public void appendGoal(Goal goal) throws IOException {
        String line = formatGoal(goal) + System.lineSeparator();
        Files.write(
            GOALS_PATH,
            line.getBytes(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    public void saveGoals(List<Goal> goals) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Goal goal : goals) {
            lines.add(formatGoal(goal));
        }
        Files.write(GOALS_PATH, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public boolean addAmountToGoal(String goalName, double amount) throws IOException {
        List<Goal> goals = loadGoals();
        boolean updated = false;
        List<Goal> updatedGoals = new ArrayList<>();

        for (Goal goal : goals) {
            if (goal.getName().equals(goalName)) {
                updatedGoals.add(new Goal(goal.getName(), goal.getCost(), goal.getCurrentAmount() + amount));
                updated = true;
            } else {
                updatedGoals.add(goal);
            }
        }

        if (updated) {
            saveGoals(updatedGoals);
        }
        return updated;
    }

    private Optional<Goal> parseGoal(String line) {
        if (line == null || line.trim().isEmpty()) {
            return Optional.empty();
        }

        int lastComma = line.lastIndexOf(',');
        if (lastComma < 0) {
            return Optional.empty();
        }

        String beforeLast = line.substring(0, lastComma);
        String currentPart = line.substring(lastComma + 1).trim();
        int secondLastComma = beforeLast.lastIndexOf(',');
        if (secondLastComma < 0) {
            return Optional.empty();
        }

        String name = beforeLast.substring(0, secondLastComma).trim();
        String costPart = beforeLast.substring(secondLastComma + 1).trim();

        try {
            double cost = Double.parseDouble(costPart);
            double current = Double.parseDouble(currentPart);
            return Optional.of(new Goal(name, cost, current));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private String formatGoal(Goal goal) {
        return goal.getName() + "," + goal.getCost() + "," + goal.getCurrentAmount();
    }
}
