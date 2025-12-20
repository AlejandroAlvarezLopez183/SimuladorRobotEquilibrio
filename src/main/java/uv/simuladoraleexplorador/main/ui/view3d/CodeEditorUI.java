package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uv.simuladoraleexplorador.main.scripting.PythonExecutor;

public class CodeEditorUI extends Stage {

    private final TextArea codeArea;
    private final TextArea consoleArea;
    private final PythonExecutor executor;

    public CodeEditorUI() {
        this.executor = new PythonExecutor();
        this.setTitle("Programación de Microcontrolador (Python)");

        // 1. Editor de Código
        codeArea = new TextArea();
        codeArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 14px;");
        codeArea.setText("print('Iniciando sistema del Robot...')\n\nfor i in range(1, 4):\n    print('Chequeando sensor ' + str(i))");

        // 2. Consola Negra (Salida)
        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(150);
        consoleArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: #00ff00; -fx-font-family: 'Monospaced';");
        consoleArea.setText(">> Sistema listo.\n");

        // 3. Botón Ejecutar
        Button btnRun = new Button("▶ EJECUTAR CÓDIGO");
        btnRun.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
        btnRun.setMaxWidth(Double.MAX_VALUE);

        btnRun.setOnAction(e -> executeCode());

        // Armar la ventana
        VBox bottomLayout = new VBox(5, btnRun, new Label("Consola:"), consoleArea);
        bottomLayout.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(codeArea);
        root.setBottom(bottomLayout);

        Scene scene = new Scene(root, 600, 500);
        this.setScene(scene);

        // Matar el proceso de Python al cerrar la ventana
        this.setOnCloseRequest(e -> executor.close());
    }

    private void executeCode() {
        consoleArea.appendText(">> Ejecutando...\n");
        String script = codeArea.getText();

        executor.runScript(script, output -> {
            // Volver al hilo visual de JavaFX para mostrar el texto
            Platform.runLater(() -> {
                consoleArea.appendText(output);
                consoleArea.appendText("\n>> Fin.\n");
            });
        });
    }
}