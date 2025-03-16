package org.jah.newsys2;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import org.jah.newsys2.backend.RecommendAI;
import org.jah.newsys2.backend.StudentEval;
import org.jah.newsys2.backend.Subject;

import java.io.IOException;
import java.util.*;

public class SubjectSelectionController {

    @FXML private VBox subjectsContainer;
    @FXML private Button submitButton;
    @FXML private Button backButton;
    @FXML private StackPane loadingPane;
    @FXML private ImageView loadingGif;

    private List<SubjectEntry> subjectEntries = new ArrayList<>();
    private String program;
    private String studentName;
    private int studentId;
    private int year;
    private int semester;
    private Set<String> selectedSubjects = new HashSet<>();

    private final Service<String> recommendationService = new Service<>() {
        @Override
        protected Task<String> createTask() {
            return new Task<>() {
                @Override
                protected String call() throws Exception {
                    Map<String, Boolean> statusMap = new HashMap<>();
                    for (SubjectEntry entry : subjectEntries) {
                        statusMap.put(entry.getSubject(), entry.isPassed());
                    }

                    StudentEval eval = new StudentEval(program);
                    List<Subject> recommendations = eval.getRecommendedSubjects(statusMap, year, semester);
                    RecommendAI ai = new RecommendAI();
                    return ai.recommendAI(recommendations, program, studentName);
                }
            };
        }
    };


    private class SubjectEntry {
        private final ComboBox<String> subjectComboBox;
        private final ComboBox<String> statusComboBox;

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

        recommendationService.setOnRunning(e -> {
            loadingPane.setVisible(true);
            loadingGif.setVisible(true);  // Changed from loadingSpinner
        });

        recommendationService.setOnSucceeded(e -> {
            loadingPane.setVisible(false);
            loadingGif.setVisible(false);  // Changed from loadingSpinner
            openRecommendedSubjectsWindow(recommendationService.getValue());
        });

        recommendationService.setOnFailed(e -> {
            loadingPane.setVisible(false);
            loadingGif.setVisible(false);  // Changed from loadingSpinner
            showAlert(AlertType.ERROR, "Error",
                    "Failed to generate recommendations: " + recommendationService.getException().getMessage());
        });
    }

    public void setupSubjects(int count, String program, String studentName, int studentId, int year, int semester) {
        this.program = program;
        this.studentName = studentName;
        this.studentId = studentId;
        this.year = year;
        this.semester = semester;

        StudentEval eval = new StudentEval(program);
        List<String> subjectCodes = eval.getAllSubjects().stream()
                .map(Subject::getCode)
                .toList();

        for (int i = 0; i < count; i++) {
            createSubjectEntry(i + 1, subjectCodes);
        }
    }

    private void createSubjectEntry(int index, List<String> subjectCodes) {
        HBox entry = new HBox(10);
        entry.setPadding(new Insets(5, 10, 5, 10));

        ComboBox<String> subjectCB = new ComboBox<>();
        subjectCB.getItems().addAll(subjectCodes);
        subjectCB.setPrefWidth(150);
        subjectCB.setPromptText("Select Subject");
        subjectCB.setOnAction(e -> handleSubjectSelection(subjectCB));

        ComboBox<String> statusCB = new ComboBox<>();
        statusCB.getItems().addAll("Pass", "Fail");
        statusCB.setPrefWidth(100);
        statusCB.setPromptText("Status");

        entry.getChildren().addAll(
                new Label("Subject " + index + ":"),
                subjectCB,
                new Label("Status:"),
                statusCB
        );

        subjectsContainer.getChildren().add(entry);
        subjectEntries.add(new SubjectEntry(subjectCB, statusCB));
    }

    private void handleSubjectSelection(ComboBox<String> changedComboBox) {
        String selected = changedComboBox.getValue();
        if (selected == null) return;

        if (selectedSubjects.contains(selected)) {
            showAlert(AlertType.ERROR, "Duplicate",
                    "Subject " + selected + " already selected!");
            changedComboBox.setValue(null);
            return;
        }
        updateSelectedSubjects();
    }

    private void updateSelectedSubjects() {
        selectedSubjects.clear();
        subjectEntries.stream()
                .map(SubjectEntry::getSubject)
                .filter(Objects::nonNull)
                .forEach(selectedSubjects::add);
    }

    private void handleSubmit(ActionEvent event) {
        // Validate all entries
        Set<String> subjects = new HashSet<>();
        Map<String, Boolean> statusMap = new HashMap<>();

        for (SubjectEntry entry : subjectEntries) {
            String subject = entry.getSubject();
            String status = entry.statusComboBox.getValue();

            if (subject == null || status == null) {
                showAlert(AlertType.ERROR, "Error", "Complete all fields!");
                return;
            }

            if (!subjects.add(subject)) {
                showAlert(AlertType.ERROR, "Error", "Duplicate: " + subject);
                return;
            }

            statusMap.put(subject, entry.isPassed());
        }

        recommendationService.restart();
    }

    private void openRecommendedSubjectsWindow(String recommendation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("recommended_subjects.fxml"));
            Parent root = loader.load();

            RecommendedSubjectsController controller = loader.getController();
            controller.setupRecommendedSubjects(studentName, studentId, program, recommendation);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            ((Stage) submitButton.getScene().getWindow()).close();
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Error", "Couldn't open window: " + e.getMessage());
        }
    }

    private void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("main_screen.fxml"))));
            stage.setMaximized(true);
        } catch (IOException e) {
            showAlert(AlertType.ERROR, "Error", "Couldn't return to main: " + e.getMessage());
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