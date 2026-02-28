package com.example.model;

public class StudySessionConfig {
    private final int studyDurationSeconds;
    private final int breakDurationSeconds;
    private final int totalSessions;

    public StudySessionConfig(int studyDurationSeconds, int breakDurationSeconds, int totalSessions) {
        this.studyDurationSeconds = Math.max(1, studyDurationSeconds);
        this.breakDurationSeconds = Math.max(1, breakDurationSeconds);
        this.totalSessions = Math.max(1, totalSessions);
    }

    public int getStudyDurationSeconds() {
        return studyDurationSeconds;
    }

    public int getBreakDurationSeconds() {
        return breakDurationSeconds;
    }

    public int getTotalSessions() {
        return totalSessions;
    }
}
