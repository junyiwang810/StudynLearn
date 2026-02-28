package com.example.service;

import com.example.model.StudySessionConfig;
import com.example.repository.SettingsRepository;

public class StudySessionConfigService {
    private final SettingsRepository settingsRepository;

    public StudySessionConfigService(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public StudySessionConfig load() {
        int studyDurationSeconds = settingsRepository.getInt(
            "studyDurationSeconds",
            settingsRepository.getInt("studyDuration", 25) * 60
        );
        int breakDurationSeconds = settingsRepository.getInt(
            "breakDurationSeconds",
            settingsRepository.getInt("breakDuration", 5) * 60
        );
        int totalSessions = settingsRepository.getInt("sessions", 1);

        return new StudySessionConfig(studyDurationSeconds, breakDurationSeconds, totalSessions);
    }
}
