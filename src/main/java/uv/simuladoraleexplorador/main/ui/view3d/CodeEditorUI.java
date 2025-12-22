package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import org.graalvm.polyglot.Value;
import uv.simuladoraleexplorador.main.scripting.CppManager;
import uv.simuladoraleexplorador.main.scripting.PythonExecutor;

import java.io.File;

public class CodeEditorUI extends Stage {

    private final TextArea codeArea;
    private final TextArea consoleArea;
    private final ComboBox<String> languageSelector; // <--- Nuevo selector

    // Los dos cerebros disponibles
    private final PythonExecutor pythonExecutor;
    private Value loopFunction;
    private AnimationTimer arduinoLoop;
    private final CppManager cppManager;

    public CodeEditorUI() {
        this.pythonExecutor = new PythonExecutor();
        this.cppManager = new CppManager();

        this.setTitle("Programación de Microcontrolador");

        // 1. Editor de Código
        codeArea = new TextArea();
        codeArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 14px;");
        // Texto inicial por defecto (Python)
        codeArea.setText("import time\n\nrobot.log('Sistema Python Iniciado')\nrobot.setMotor(1, 100)");

        // 2. Consola
        consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefHeight(150);
        consoleArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: #00ff00; -fx-font-family: 'Monospaced';");
        consoleArea.setText(">> Listo. Selecciona un lenguaje y ejecuta.\n");

        // 3. Selector de Lenguaje
        languageSelector = new ComboBox<>();
        languageSelector.getItems().addAll("Python", "C++ (Arduino)");
        languageSelector.setValue("Python"); // Seleccionado por defecto

        // Cambiar el código de ejemplo cuando cambias de lenguaje
        languageSelector.setOnAction(e -> updateExampleCode());

        // 4. Botón Ejecutar
        Button btnRun = new Button("▶ SUBIR CÓDIGO");
        btnRun.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnRun.setMaxWidth(Double.MAX_VALUE);
        btnRun.setOnAction(e -> executeCode());

        // Barra de herramientas superior (Selector + Botón)
        HBox tools = new HBox(10, new Label("Lenguaje:"), languageSelector, btnRun);
        HBox.setHgrow(btnRun, Priority.ALWAYS); // Que el botón ocupe el espacio sobrante
        tools.setPadding(new Insets(0, 0, 10, 0));

        // Armar ventana
        VBox bottomLayout = new VBox(5, new Label("Consola:"), consoleArea);
        bottomLayout.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(tools);       // Arriba: Botones
        root.setCenter(codeArea); // Centro: Editor
        root.setBottom(bottomLayout); // Abajo: Consola

        Scene scene = new Scene(root, 600, 550);
        this.setScene(scene);

        this.setOnCloseRequest(e -> {
            if (arduinoLoop != null) arduinoLoop.stop(); // <--- AGREGAR ESTO
            pythonExecutor.close();
            cppManager.close();
        });
    }

    // Cambia el texto de ejemplo según el lenguaje para no escribir desde cero
    private void updateExampleCode() {
        String lang = languageSelector.getValue();
        if ("Python".equals(lang)) {
            codeArea.setText("import time\n\nrobot.log('Modo Python Activo')\n# Escribe tu script aquí...");
        } else {
            codeArea.setText("#include <stdio.h>\n\nextern \"C\" {\n    void setup() {\n        printf(\"Modo C++ Activo\\n\");\n    }\n    void loop() {\n    }\n}");
        }
    }

    private void executeCode() {
        String lang = languageSelector.getValue();
        String code = codeArea.getText();

        if ("Python".equals(lang)) {
            // --- MODO PYTHON ---
            consoleArea.appendText("\n>> Ejecutando script Python...\n");
            pythonExecutor.runScript(code, output -> {
                Platform.runLater(() -> consoleArea.appendText(output));
            });

        } else {
            if (arduinoLoop != null) {
                arduinoLoop.stop();
            }

            consoleArea.appendText("\n>> Compilando Sketch...\n");

            new Thread(() -> {
                // A. Compilar
                File bitcode = cppManager.compile(code, msg -> {
                    Platform.runLater(() -> consoleArea.appendText(msg));
                });

                if (bitcode != null) {
                    Platform.runLater(() -> {
                        // B. Cargar en memoria
                        Value bindings = cppManager.loadBindings(bitcode, msg -> consoleArea.appendText(msg));

                        if (bindings != null) {
                            startArduinoSimulation(bindings);
                        }
                    });
                }
            }).start();
        }
    }
    private void startArduinoSimulation(Value bindings) {
        // 1. Buscar setup() y loop()
        Value setupFunc = bindings.getMember("setup");
        this.loopFunction = bindings.getMember("loop");

        // 2. Ejecutar setup() UNA VEZ
        if (setupFunc != null && setupFunc.canExecute()) {
            consoleArea.appendText(">> Ejecutando setup()...\n");
            try {
                setupFunc.executeVoid();
            } catch (Exception e) {
                consoleArea.appendText("Error en setup: " + e.getMessage() + "\n");
                return; // Si falla setup, no iniciamos loop
            }
        }

        // 3. Iniciar el Loop Infinito (60 veces por segundo)
        if (loopFunction != null && loopFunction.canExecute()) {
            consoleArea.appendText(">> Iniciando loop()...\n");

            arduinoLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    try {
                        // AQUÍ LLAMAMOS A C++ EN CADA FRAME
                        loopFunction.executeVoid();
                    } catch (Exception e) {
                        consoleArea.appendText("Crash en loop: " + e.getMessage() + "\n");
                        this.stop(); // Detener si hay error
                    }
                }
            };
            arduinoLoop.start(); // ¡Arranca el motor!
        } else {
            consoleArea.appendText(">> Aviso: No se encontró void loop().\n");
        }
    }
}