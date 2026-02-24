package com.example.service;

public final class StudyRateService {
    private StudyRateService() {
    }

    public static double calculate(double hourlyRate, double balance) {
        double safeHourly = Math.max(0.0, hourlyRate);
        double safeBalance = Math.max(0.0, balance);
        double factor = 1.1 + (0.9 / (1.0 + (safeBalance / 2000.0)));
        return safeHourly * factor;
    }
}
