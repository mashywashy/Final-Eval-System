package org.jah.newsys2;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.jah.newsys2.backend.StudentEval;
import org.jah.newsys2.backend.Subject;

import java.io.IOException;
import java.util.List;

public class AppController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField idNumberField;

    @FXML
    private ComboBox<String> newStudentComboBox;

    @FXML
    private ComboBox<String> programComboBox;

    @FXML
    private TextField subjectsField;

    @FXML
    private Button nextButton;
    @FXML
    private Button cancelButton;

    @FXML
    public void initialize() {
        // Initialize ComboBoxes
        newStudentComboBox.getItems().addAll("Yes", "No");
        programComboBox.getItems().addAll("BSIT", "BSA", "BSN", "BSMT");

        // Add listener to newStudentComboBox to enable/disable subjectsField
        newStudentComboBox.setOnAction(event -> handleNewStudentSelection());

        // Handle Next button click
        nextButton.setOnAction(event -> handleNextButton());
        cancelButton.setOnAction(event -> handleCancelButton());
    }

    private void handleNewStudentSelection() {
        String selection = newStudentComboBox.getValue();
        if ("No".equals(selection)) {
            subjectsField.setDisable(false);
        } else {
            subjectsField.setDisable(true);
            subjectsField.clear();
        }
    }

    private void handleNextButton() {
        String name = nameField.getText();
        String id = idNumberField.getText();
        String isNewStudent = newStudentComboBox.getValue();
        String program = programComboBox.getValue();

        // Validate input fields
        if (name.isEmpty() || id.isEmpty() || isNewStudent == null || program == null) {
            showAlert("Error", "Please complete all required fields.");
            return;
        }

        if ("Yes".equals(isNewStudent)) {
            // For new students, directly show recommendations
            StudentEval se = new StudentEval(program);
            List<Subject> recommendedSubjects = se.getRecommendedSubjects();
            displayRecommendedSubjects(name, id, recommendedSubjects);
        } else {
            // For continuing students, check the subjects count field
            if (subjectsField.getText().isEmpty() || !isNumeric(subjectsField.getText())) {
                showAlert("Error", "Please enter a valid number of subjects taken.");
                return;
            }

            if(Integer.parseInt(subjectsField.getText()) > 15) {
                showAlert("Error", "Please enter no more than 15");
                return;
            }

            int subjectCount = Integer.parseInt(subjectsField.getText());
            if (subjectCount <= 0) {
                showAlert("Error", "Number of subjects must be greater than zero.");
                return;
            }

            // Open the subject selection screen
            openSubjectSelectionScreen(subjectCount, program, name, id);
        }
    }

    private void openSubjectSelectionScreen(int subjectCount, String program, String name, String id) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("subject_input.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();


            if (controller instanceof SubjectSelectionController) {
                ((SubjectSelectionController) controller).setupSubjects(subjectCount, program, name, id);
            } else {
                showAlert("Error", "Unexpected controller: " + controller.getClass().getName());
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Subject Selection");
            stage.setScene(new Scene(root, 600, 400));
            stage.setMaximized(true);
            stage.show();

            // Close the current window
            Stage currentStage = (Stage) nextButton.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open subject selection screen: " + e.getMessage());
        }
    }

    // This method replaces the existing displayRecommendedSubjects method in AppController.java
    private void displayRecommendedSubjects(String name, String id, List<Subject> recommendedSubjects) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("recommended_subjects.fxml"));
            Parent root = loader.load();

            RecommendedSubjectsController controller = loader.getController();
            controller.setupRecommendedSubjects(name, id, programComboBox.getValue(), recommendedSubjects);

            Stage stage = new Stage();
            stage.setTitle("Recommended Subjects");
            stage.setScene(new Scene(root, 600, 400));
            stage.setMaximized(true);
            stage.show();



            // Close the current window
            Stage currentStage = (Stage) nextButton.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open recommended subjects screen: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleCancelButton() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}