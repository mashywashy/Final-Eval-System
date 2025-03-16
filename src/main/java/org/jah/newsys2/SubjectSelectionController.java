package org.jah.newsys2;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import org.jah.newsys2.backend.StudentEval;
import org.jah.newsys2.backend.Subject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubjectSelectionController {

    @FXML
    private VBox subjectsContainer;

    @FXML
    private Button submitButton;

    @FXML
    private Button backButton;

    private List<SubjectEntry> subjectEntries = new ArrayList<>();
    private String program;
    private String studentName;
    private int studentId;
    private int year;
    private int semester;
    private Set<String> selectedSubjects = new HashSet<>(); // Track selected subjects

    private class SubjectEntry {
        private ComboBox<String> subjectComboBox;
        private ComboBox<String> statusComboBox;

        public SubjectEntry(ComboBox<String> subjectComboBox, ComboBox<String> statusComboBox) {
            this.subjectComboBox = subjectComboBox;
            this.statusComboBox = statusComboBox;
        }

        public String getSubject() {
            return subjectComboBox.getValue();
        }

        public boolean isPassed() {
            return "Pass".equals(statusComboBox.getValue());
        }
    }

    @FXML
    public void initialize() {
        submitButton.setOnAction(this::handleSubmit);
        backButton.setOnAction(this::handleBack);
    }

    public void setupSubjects(int count, String program, String studentName, int studentId, int year, int semester) {
        this.program = program;
        this.studentName = studentName;
        this.studentId = studentId;
        this.year = year;
        this.semester = semester;

        // Get all subjects for the program to populate the ComboBoxes
        StudentEval studentEval = new StudentEval(program);
        List<Subject> allProgramSubjects = studentEval.getAllSubjects();
        List<String> subjectCodes = new ArrayList<>();

        for (Subject subject : allProgramSubjects) {
            subjectCodes.add(subject.getCode());
        }

        // Create the specified number of subject entries
        for (int i = 0; i < count; i++) {
            createSubjectEntry(i + 1, subjectCodes);
        }
    }

    private void createSubjectEntry(int index, List<String> subjectCodes) {
        // Create components for this subject entry
        HBox entryContainer = new HBox(10);
        entryContainer.setPadding(new Insets(5, 10, 5, 10));

        Label indexLabel = new Label("Subject " + index + ":");
        indexLabel.setPrefWidth(80);

        ComboBox<String> subjectComboBox = new ComboBox<>();
        subjectComboBox.getItems().addAll(subjectCodes);
        subjectComboBox.setPrefWidth(150);
        subjectComboBox.setPromptText("Select Subject");

        // Add listener to handle subject selection and prevent duplicates
        subjectComboBox.setOnAction(e -> handleSubjectSelection(subjectComboBox));

        Label statusLabel = new Label("Status:");
        statusLabel.setPrefWidth(50);

        ComboBox<String> statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("Pass", "Fail");
        statusComboBox.setPrefWidth(100);
        statusComboBox.setPromptText("Status");

        // Add components to the container
        entryContainer.getChildren().addAll(indexLabel, subjectComboBox, statusLabel, statusComboBox);
        subjectsContainer.getChildren().add(entryContainer);

        // Store the entry for later retrieval
        subjectEntries.add(new SubjectEntry(subjectComboBox, statusComboBox));
    }

    private void handleSubjectSelection(ComboBox<String> changedComboBox) {
        String selectedSubject = changedComboBox.getValue();

        if (selectedSubject == null) {
            return;
        }

        // If this subject was already selected in another combo box, show error and reset
        if (selectedSubjects.contains(selectedSubject)) {
            for (SubjectEntry entry : subjectEntries) {
                if (entry.subjectComboBox != changedComboBox &&
                        selectedSubject.equals(entry.subjectComboBox.getValue())) {
                    showAlert(AlertType.ERROR, "Duplicate Selection",
                            "Subject '" + selectedSubject + "' is already selected. Please choose a different subject.");
                    changedComboBox.setValue(null);
                    return;
                }
            }
        }

        // Update selected subjects set
        updateSelectedSubjects();
    }

    private void updateSelectedSubjects() {
        // Clear and rebuild the set of selected subjects
        selectedSubjects.clear();

        for (SubjectEntry entry : subjectEntries) {
            String subject = entry.getSubject();
            if (subject != null) {
                selectedSubjects.add(subject);
            }
        }
    }

    private void handleSubmit(ActionEvent event) {
        // Validate entries
        boolean isValid = true;
        Set<String> subjectSet = new HashSet<>();

        for (SubjectEntry entry : subjectEntries) {
            String subject = entry.getSubject();

            // Check if all fields are filled
            if (subject == null || entry.statusComboBox.getValue() == null) {
                isValid = false;
                showAlert(AlertType.ERROR, "Error", "Please complete all subject entries.");
                return;
            }

            // Check for duplicates (extra validation)
            if (!subjectSet.add(subject)) {
                isValid = false;
                showAlert(AlertType.ERROR, "Error", "Duplicate subject found: " + subject +
                        ". Each subject can only be entered once.");
                return;
            }
        }

        if (!isValid) {
            return;
        }

        // Create map of subjects and their pass/fail status
        Map<String, Boolean> subjectStatusMap = new HashMap<>();
        for (SubjectEntry entry : subjectEntries) {
            subjectStatusMap.put(entry.getSubject(), entry.isPassed());
        }

        // Get recommended subjects based on the student's results
        StudentEval studentEval = new StudentEval(program);
        List<Subject> recommendedSubjects = studentEval.getRecommendedSubjects(subjectStatusMap, year, semester);

        // Pass data to RecommendedSubjectsController
        openRecommendedSubjectsWindow(recommendedSubjects);
    }

    private void handleBack(ActionEvent event) {
        try {
            // Load the main screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("main_screen.fxml"));
            Parent root = loader.load();

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();

            // Create new scene with the main screen
            Scene scene = new Scene(root);

            // Get the current stage and set the new scene
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Navigation Error", "Could not return to main screen.");
        }
    }

    private void openRecommendedSubjectsWindow(List<Subject> recommendedSubjects) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("recommended_subjects.fxml"));
            Parent root = loader.load();

            RecommendedSubjectsController controller = loader.getController();
            controller.setupRecommendedSubjects(studentName, studentId, program, recommendedSubjects);

            Stage stage = new Stage();
            stage.setTitle("Recommended Subjects");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            // Close the current window
            ((Stage) submitButton.getScene().getWindow()).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}