package uv.simuladoraleexplorador.main.ui;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class SplashController {

    @FXML
    private Label lblStatus;

    @FXML
    private ProgressBar progressBar;

    // Métodos para actualizar la interfaz desde la App principal
    public void updateProgress(double progress, String message) {
        progressBar.setProgress(progress);
        lblStatus.setText(message);
    }
}