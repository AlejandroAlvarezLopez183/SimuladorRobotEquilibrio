package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;
import uv.simuladoraleexplorador.main.model.components.ComponentType;
import uv.simuladoraleexplorador.main.model.components.logic.Wire;
import uv.simuladoraleexplorador.main.model.components.logic.WiringManager;
import uv.simuladoraleexplorador.main.utils.DataManager;

public class WiringUI extends Stage {

    // Origen (Microcontrolador)
    private ComboBox<ElectronicComponent> cmbSourceBoard;
    private ListView<Integer> listSourcePins;

    // Destino (Motores/Sensores)
    private ComboBox<ElectronicComponent> cmbTargetComp;
    private ListView<String> listTargetInputs;
    private final RobotManager robotManager;
    // Lista de Conexiones Actuales
    private ListView<Wire> listActiveConnections;

    public WiringUI(RobotManager robotManager) {
        this.robotManager = robotManager;
        this.setTitle("🔌 Gestión de Cableado");

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #2b2b2b;");

        // --- PANEL DE SELECCIÓN ---
        HBox selectionPanel = new HBox(20);

        // LADO IZQUIERDO: MICROCONTROLADOR
        VBox leftBox = new VBox(5, new Label("1. Salida (Microcontrolador)"), createSourcePanel());
        leftBox.setStyle("-fx-border-color: #666; -fx-padding: 10;");

        // LADO DERECHO: COMPONENTE
        VBox rightBox = new VBox(5, new Label("2. Entrada (Componente)"), createTargetPanel());
        rightBox.setStyle("-fx-border-color: #666; -fx-padding: 10;");

        selectionPanel.getChildren().addAll(leftBox, rightBox);

        // --- BOTÓN CONECTAR ---
        Button btnConnect = new Button("🔗 REALIZAR CONEXIÓN");
        btnConnect.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        btnConnect.setMaxWidth(Double.MAX_VALUE);
        btnConnect.setOnAction(e -> makeConnection());

        // --- LISTA DE CABLES EXISTENTES ---
        listActiveConnections = new ListView<>();
        listActiveConnections.setPrefHeight(100);
        refreshConnectionsList();

        listSourcePins.setStyle("-fx-control-inner-background: #333; -fx-text-fill: white;");
        listTargetInputs.setStyle("-fx-control-inner-background: #333; -fx-text-fill: white;");
        listActiveConnections.setStyle("-fx-control-inner-background: #333; -fx-text-fill: white;");

        cmbSourceBoard.setStyle("-fx-base: #444; -fx-text-fill: white;");
        cmbTargetComp.setStyle("-fx-base: #444; -fx-text-fill: white;");

        Button btnDelete = new Button("Cortar Cable Seleccionado");
        btnDelete.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        btnDelete.setOnAction(e -> deleteConnection());

        // Armar todo
        root.getChildren().addAll(
                new Label("Panel de Conexiones"),
                selectionPanel,
                btnConnect,
                new Separator(),
                new Label("Cables Activos:"),
                listActiveConnections,
                btnDelete
        );

        this.setScene(new Scene(root, 600, 500));
    }

    private VBox createSourcePanel() {
        // Solo mostramos componentes que sean MICROCONTROLADORES
        cmbSourceBoard = new ComboBox<>();
        // Aquí deberías cargar desde tu DataManager o RobotManager los componentes activos
        // Por ahora simulamos carga:
        loadMicrocontrollers();

        listSourcePins = new ListView<>();
        listSourcePins.setPrefHeight(150);

        // Cuando seleccionas una placa, se llenan los pines (0 al 13)
        cmbSourceBoard.setOnAction(e -> {
            listSourcePins.getItems().clear();
            ElectronicComponent sel = cmbSourceBoard.getValue();
            if (sel != null) {
                // Aquí podrías leer del JSON cuantos pines tiene. Por defecto pongo 14.
                for (int i = 0; i < 14; i++) listSourcePins.getItems().add(i);
            }
        });

        return new VBox(5, cmbSourceBoard, new Label("Selecciona Pin Digital:"), listSourcePins);
    }

    private VBox createTargetPanel() {
        // Mostramos todo lo que NO sea microcontrolador (Motores, LEDs)
        cmbTargetComp = new ComboBox<>();
        loadTargets();

        listTargetInputs = new ListView<>();
        listTargetInputs.setPrefHeight(150);

        // Cuando seleccionas un motor, muestra sus "Puertos de Entrada"
        cmbTargetComp.setOnAction(e -> {
            listTargetInputs.getItems().clear();
            ElectronicComponent sel = cmbTargetComp.getValue();
            if (sel != null) {
                // Definimos entradas según el tipo
                if (sel.getType() == ComponentType.DC_MOTOR) {
                    listTargetInputs.getItems().addAll("ENABLE (On/Off)", "PWM (Velocidad)");
                } else if (sel.getType() == ComponentType.LED) {
                    listTargetInputs.getItems().add("ANODE (Voltaje)");
                }
            }
        });

        return new VBox(5, cmbTargetComp, new Label("Selecciona Entrada:"), listTargetInputs);
    }

    private void makeConnection() {
        ElectronicComponent source = cmbSourceBoard.getValue();
        Integer pin = listSourcePins.getSelectionModel().getSelectedItem();
        ElectronicComponent target = cmbTargetComp.getValue();
        String input = listTargetInputs.getSelectionModel().getSelectedItem();

        if (source != null && pin != null && target != null && input != null) {
            Wire wire = new Wire(source, pin, target, input);
            WiringManager.getInstance().addConnection(wire);
            refreshConnectionsList();
        }
    }

    private void deleteConnection() {
        Wire selected = listActiveConnections.getSelectionModel().getSelectedItem();
        if (selected != null) {
            WiringManager.getInstance().removeConnection(selected);
            refreshConnectionsList();
        }
    }

    private void refreshConnectionsList() {
        listActiveConnections.getItems().clear();
        listActiveConnections.getItems().addAll(WiringManager.getInstance().getConnections());
    }

    private void loadMicrocontrollers() {
        cmbSourceBoard.getItems().clear();
        // Iteramos sobre los componentes reales
        for (ElectronicComponent c : robotManager.getAllComponents()) {
            if (c.getType() == ComponentType.MICROCONTROLLER) {
                cmbSourceBoard.getItems().add(c);
            }
        }
    }

    private void loadTargets() {
        cmbTargetComp.getItems().clear();
        for (ElectronicComponent c : robotManager.getAllComponents()) {
            // Todo lo que NO sea microcontrolador es un destino válido (Motores, LEDs)
            if (c.getType() != ComponentType.MICROCONTROLLER) {
                cmbTargetComp.getItems().add(c);
            }
        }
    } }