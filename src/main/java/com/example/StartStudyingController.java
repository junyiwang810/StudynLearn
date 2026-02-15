package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class StartStudyingController {

    private Button selectedDurationBtn = null;
    private Button selectedBreakBtn = null;
    private Button selectedSessionBtn = null;
    private int studyTime = 0;
    private int breakTime = 0;
    private int sessions = 0;

    @FXML
    private void handleDuration(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        
        // Deselect previous
        if (selectedDurationBtn != null) {
            selectedDurationBtn.getStyleClass().remove("pill-green");
        }
        
        // Select new
        clicked.getStyleClass().add("pill-green");
        selectedDurationBtn = clicked;
        
        // Parse time from text (e.g., "25 min" -> 25)
        String text = clicked.getText().split(" ")[0];
        studyTime = Integer.parseInt(text);
    }

    @FXML
    private void handleBreak(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        
        if (selectedBreakBtn != null) {
            selectedBreakBtn.getStyleClass().remove("pill-green");
        }
        
        clicked.getStyleClass().add("pill-green");
        selectedBreakBtn = clicked;
        
        String text = clicked.getText().split(" ")[0];
        breakTime = Integer.parseInt(text);
    }

    @FXML
    private void handleSessions(ActionEvent event) {
        Button clicked = (Button) event.getSource();

        if (selectedSessionBtn != null) {
            selectedSessionBtn.getStyleClass().remove("pill-green");
        }

        clicked.getStyleClass().add("pill-green");
        selectedSessionBtn = clicked;

        sessions = Integer.parseInt(clicked.getText());
    }

    @FXML
    private void startTimer() throws IOException {
        if (studyTime == 0 || breakTime == 0 || sessions == 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Selection Required");
            alert.setHeaderText(null);
            alert.setContentText("Please select study duration, break duration, and number of sessions.");
            alert.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
            alert.showAndWait();
            return;
        }

        // Save settings for the Timer page to read
        saveTimerSettings(studyTime, breakTime, sessions);

        // Switch to Timer page
        App.setRoot("timer");
    }

    @FXML
    private void switchToPrimary() throws IOException {
        App.setRoot("primary");
    }

    private void saveTimerSettings(int study, int breakTime, int sessions) {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("settings.properties")) {
            props.load(in);
        } catch (IOException e) {
            // Ignore if file doesn't exist yet
        }

        props.setProperty("studyDuration", String.valueOf(study));
        props.setProperty("breakDuration", String.valueOf(breakTime));
        props.setProperty("sessions", String.valueOf(sessions));

        try (FileOutputStream out = new FileOutputStream("settings.properties")) {
            props.store(out, "Updated Timer Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}