package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import uv.simuladoraleexplorador.main.model.components.logic.Wire;
import uv.simuladoraleexplorador.main.model.components.logic.WiringManager;

public class WiringUI extends Stage {

    // Ya no necesitamos RobotManager aquí porque no vamos a crear conexiones nuevas
    // Solo leemos las que existen en el WiringManager (Singleton)

    private ListView<Wire> listActiveConnections;

    // Cambiamos el constructor: Ya no pide RobotManager
    public WiringUI() {
        this.setTitle("🧐 Inspector de Conexiones");

        VBox root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #2b2b2b;");

        // Título
        Label lblInfo = new Label("Cables Activos en la Escena:");
        lblInfo.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        // --- SOLO LA LISTA ---
        listActiveConnections = new ListView<>();
        listActiveConnections.setPrefHeight(300);
        listActiveConnections.setStyle("-fx-control-inner-background: #333; -fx-text-fill: white;");

        // Placeholder por si no hay cables
        listActiveConnections.setPlaceholder(new Label("No hay conexiones activas"));

        refreshConnectionsList();

        // --- BOTONES DE GESTIÓN ---
        Button btnDelete = new Button("✂️ Cortar Cable Seleccionado");
        btnDelete.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setOnAction(e -> deleteConnection());

        Button btnRefresh = new Button("🔄 Actualizar Lista");
        btnRefresh.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        btnRefresh.setMaxWidth(Double.MAX_VALUE);
        btnRefresh.setOnAction(e -> refreshConnectionsList());

        root.getChildren().addAll(lblInfo, listActiveConnections, btnDelete, btnRefresh);

        this.setScene(new Scene(root, 400, 450));
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
}