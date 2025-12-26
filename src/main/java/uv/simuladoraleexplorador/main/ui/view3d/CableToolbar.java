package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class CableToolbar extends HBox {

    private final WiringHandler wiringHandler;

    public CableToolbar(WiringHandler wiringHandler) {
        this.wiringHandler = wiringHandler;

        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #2b2b2b; -fx-border-color: #444; -fx-border-width: 1 0 0 0;");
        this.setPrefHeight(50);

        // Etiqueta
        Label lblTitle = new Label("CONEXIONES:");
        lblTitle.setStyle("-fx-text-fill: #aaa; -fx-font-weight: bold;");

        // Botón 1: Cursor Normal (Cancelar cable)
        Button btnCursor = createButton("🖱️", "Modo Selección (Esc)");
        btnCursor.setOnAction(e -> wiringHandler.cancelWiring());

        // Botón 2: Cable de Conexión (El Rayo)
        Button btnCable = createButton("⚡", "Conectar Componentes");
        btnCable.setStyle("-fx-background-color: #e65100; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnCable.setOnAction(e -> wiringHandler.startWiringMode());

        // Espaciador para empujar info a la derecha
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Label de Estado (Ej: "Selecciona el Origen...")
        Label lblStatus = new Label("Listo");
        lblStatus.textProperty().bind(wiringHandler.statusMessageProperty());
        lblStatus.setStyle("-fx-text-fill: cyan;");

        this.getChildren().addAll(lblTitle, btnCursor, btnCable, spacer, lblStatus);
    }

    private Button createButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setPrefSize(40, 30);
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }
}