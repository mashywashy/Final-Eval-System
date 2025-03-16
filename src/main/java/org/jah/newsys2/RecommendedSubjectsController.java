package org.jah.newsys2;

import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class RecommendedSubjectsController {

    // UI Components
    @FXML private Label nameLabel;
    @FXML private TextArea recommendationTextArea;
    @FXML private Label idLabel;
    @FXML private Label programLabel;
    @FXML private Button printButton;
    @FXML private Button closeButton;

    @FXML
    public void initialize() {
        // Configure text area properties
        recommendationTextArea.setEditable(false);
        recommendationTextArea.setWrapText(true);

        // Set button actions
        printButton.setOnAction(e -> handlePrint());
        closeButton.setOnAction(e -> handleClose());
    }

    // Updated to match string-based recommendation
    public void setupRecommendedSubjects(String name, int id, String program, String recommendation) {
        nameLabel.setText(name);
        idLabel.setText(String.valueOf(id));
        programLabel.setText(program);
        recommendationTextArea.setText(recommendation);
    }

    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(printButton.getScene().getWindow())) {
            boolean success = job.printPage(recommendationTextArea);
            if (success) job.endJob();
        }
    }

    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}