package org.jah.newsys2;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jah.newsys2.backend.RecommendAI;
import org.jah.newsys2.backend.StudentEval;
import org.jah.newsys2.backend.Subject;

import java.io.IOException;
import java.util.List;

public class AppController {

    @FXML private TextField nameField;
    @FXML private TextField idNumberField;
    @FXML private ComboBox<String> newStudentComboBox;
    @FXML private ComboBox<String> programComboBox;
    @FXML private ComboBox<String> yearLevelComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private TextField subjectsField;
    @FXML private Button nextButton;
    @FXML private Button cancelButton;

    @FXML private StackPane loadingPane;
    @FXML private ImageView loadingGif;
    private List<Subject> currentRecommendedSubjects;

    // Add the recommendation service
    private final Service<String> recommendationService = new Service<>() {
        @Override
        protected Task<String> createTask() {
            // Capture values at time of task creation
            final String name = nameField.getText();
            final String program = programComboBox.getValue();
            final List<Subject> subjects = currentRecommendedSubjects;

            return new RecommendationTask(name, program, subjects);
        }
    };

    // Add this inner class
    private static class RecommendationTask extends Task<String> {
        private final String name;
        private final String program;
        private final List<Subject> recommendedSubjects;

        public RecommendationTask(String name, String program, List<Subject> recommendedSubjects) {
            this.name = name;
            this.program = program;
            this.recommendedSubjects = recommendedSubjects;
        }

        @Override
        protected String call() {
            RecommendAI recommender = new RecommendAI();
            return recommender.recommendAI(recommendedSubjects, program, name);
        }
    }

    @FXML
    public void initialize() {
        // Initialize ComboBoxes
        newStudentComboBox.getItems().addAll("Yes", "No");
        programComboBox.getItems().addAll("BSIT", "BSA", "BSN", "BSMT");
        yearLevelComboBox.getItems().addAll("1", "2", "3", "4");
        semesterComboBox.getItems().addAll("1", "2");

        // Add listener to newStudentComboBox to enable/disable subjectsField
        newStudentComboBox.setOnAction(_ -> updateSubjectsFieldState());

        // Add listeners to year and semester ComboBoxes
        yearLevelComboBox.setOnAction(_ -> updateSubjectsFieldState());
        semesterComboBox.setOnAction(_ -> updateSubjectsFieldState());

        // Handle Next button click
        nextButton.setOnAction(_ -> handleNextButton());
        cancelButton.setOnAction(_ -> handleCancelButton());


        // Initialize service handlers
        recommendationService.setOnRunning(_ -> {
            loadingPane.setVisible(true);
            loadingGif.setVisible(true);
        });

        recommendationService.setOnSucceeded(_ -> {
            loadingPane.setVisible(false);
            String recommendation = recommendationService.getValue();
            openRecommendationWindowAfterLoading(
                    nameField.getText(),
                    idNumberField.getText(),
                    programComboBox.getValue(),
                    recommendation
            );
        });

        recommendationService.setOnFailed(e -> {
            loadingPane.setVisible(false);
            showAlert("Error", "Failed to generate recommendations: " + e.getSource().getException().getMessage());
        });
    }

    private void displayRecommendedSubjects(String id, List<Subject> recommendedSubjects) {
        if (!isNumeric(id)) {
            showAlert("Error", "ID Number must contain only digits.");
            return;
        }

        currentRecommendedSubjects = recommendedSubjects;
        recommendationService.restart();
    }

    // Separate method for opening the recommendation window after loading
    private void openRecommendationWindowAfterLoading(String name, String id, String program, String recommendation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("recommended_subjects.fxml"));
            Parent root = loader.load();

            RecommendedSubjectsController controller = loader.getController();
            controller.setupRecommendedSubjects(name, Integer.parseInt(id), program, recommendation);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            // Close current window
            ((Stage) nextButton.getScene().getWindow()).close();
        } catch (IOException e) {
            showAlert("Error", "Could not open recommendation window: " + e.getMessage());
        }
    }

    private void updateSubjectsFieldState() {
        boolean isNewStudent = "Yes".equals(newStudentComboBox.getValue());

        // Disable subjects field if new student OR first year first semester
        subjectsField.setDisable(isNewStudent);

        // If disabled and not already cleared by newStudentComboBox, clear it

        // Only disable year and semester combo boxes for new students
        yearLevelComboBox.setDisable(isNewStudent);
        semesterComboBox.setDisable(isNewStudent);

        if (isNewStudent) {
            yearLevelComboBox.setValue(null);
            semesterComboBox.setValue(null);
        }
    }

    private void handleNextButton() {
        String name = nameField.getText();
        String id = idNumberField.getText();
        String isNewStudent = newStudentComboBox.getValue();
        String program = programComboBox.getValue();
        String yearLevel = yearLevelComboBox.getValue();
        String semester = semesterComboBox.getValue();

        // Validate input fields
        if (name.isEmpty() || id.isEmpty() || isNewStudent == null || program == null) {
            showAlert("Error", "Please complete all required fields.");
            return;
        }
        if (!isAlphanumeric(name)) {
            showAlert("Error", "Name must not contain illegal characters.");
            return;
        }

        // Validate that ID is numeric
        if (!isNumeric(id)) {
            showAlert("Error", "ID Number must contain only digits.");
            return;
        }

        boolean isFirstYearFirstSem = "1".equals(yearLevel) && "1".equals(semester);

        if ("No".equals(isNewStudent) && !isFirstYearFirstSem) {
            if (subjectsField.getText().isEmpty() || !isNumeric(subjectsField.getText())) {
                showAlert("Error", "Please enter a valid number of subjects taken.");
                return;
            }

            int subjectCount = Integer.parseInt(subjectsField.getText());
            int yearLevelInt = Integer.parseInt(yearLevel);

            if (yearLevelInt <= 3 && subjectCount < 8) {
                showAlert("Error", "Students in year 3 and below must take at least 8 subjects.");
                return;
            }

            if (subjectCount <= 0 || subjectCount > 8) {
                showAlert("Error", "You can take between 1 and 8 subjects only.");
                return;
            }
        }

        // For new students, show recommendations directly
        if ("Yes".equals(isNewStudent)) {
            StudentEval se = new StudentEval(program);
            List<Subject> recommendedSubjects = se.getRecommendedSubjects();
            displayRecommendedSubjects(id, recommendedSubjects);
        }
        // For continuing students, check the subjects count field
        else {
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
            openSubjectSelectionScreen(subjectCount, program, name, id, yearLevel, semester);
        }
    }

    private void openSubjectSelectionScreen(int subjectCount, String program, String name, String id, String year, String semester) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("subject_input.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof SubjectSelectionController) {
                ((SubjectSelectionController) controller).setupSubjects(subjectCount, program, name, Integer.parseInt(id), Integer.parseInt(year), Integer.parseInt(semester));
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

    private boolean isAlphanumeric(String str) {
        // Matches only letters and spaces, no numbers
        return str.matches("[a-zA-Z ]+");
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