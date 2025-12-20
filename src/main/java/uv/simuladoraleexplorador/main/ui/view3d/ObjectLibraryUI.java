package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;

public class ObjectLibraryUI {

    private final RobotManager robotManager;
    private final VBox container;
    private final javafx.scene.control.ListView<ElectronicComponent> componentsList = new javafx.scene.control.ListView<>();
    public ObjectLibraryUI(RobotManager robotManager) {
        this.robotManager = robotManager;
        this.container = new VBox(10);
        this.container.setPadding(new Insets(10));
        buildUI();
    }

    private void buildUI() {
        // --- 1. SECCIÓN: OBJETOS PRIMITIVOS (ESTÁNDAR) ---
        Label title = new Label("Objetos de Prueba");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button btnCube = createButton("Cubo (10u)", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createCube(10), RobotManager.ShapeType.BOX, 10, 10);
        });

        Button btnSphere = createButton("Esfera (8u)", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createSphere(8), RobotManager.ShapeType.SPHERE, 8, 0);
        });

        Button btnCylinder = createButton("Cilindro", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createCylinder(5, 15), RobotManager.ShapeType.CYLINDER, 5, 15);
        });

        Button btnRamp = createButton("Rampa", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createRamp(15), RobotManager.ShapeType.RAMP, 15, 15);
        });

        // --- 2. SECCIÓN: MIS COMPONENTES (PERSONALIZADOS) ---
        Label lblComp = new Label("Mis Componentes");
        lblComp.setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        Button btnNewComp = new Button("+ Crear Nuevo Componente");
        btnNewComp.setMaxWidth(Double.MAX_VALUE);
        btnNewComp.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-cursor: hand;");

        // Acción del botón: Abre el formulario
        btnNewComp.setOnAction(e -> handleNewComponent());

        // --- 3. CONFIGURACIÓN DE LA LISTA VISUAL ---
        componentsList.setPrefHeight(150);
        componentsList.setStyle("-fx-background-color: #333; -fx-control-inner-background: #333; -fx-text-fill: white;");

        // Evento: Doble Clic (Spawn)
        componentsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ElectronicComponent selected = componentsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    robotManager.spawnComponent(selected);
                }
            }
        });

        // --- NUEVO: MENÚ CONTEXTUAL (Clic Derecho para Borrar) ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Eliminar Componente");
        deleteItem.setStyle("-fx-text-fill: red;"); // Un toque visual de peligro

        deleteItem.setOnAction(e -> {
            ElectronicComponent selected = componentsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                confirmAndDelete(selected);
            }
        });

        contextMenu.getItems().add(deleteItem);
        componentsList.setContextMenu(contextMenu);

        try {
            java.util.List<ElectronicComponent> savedComps = uv.simuladoraleexplorador.main.utils.DataManager.loadAllComponents();
            if (savedComps != null) {
                componentsList.getItems().addAll(savedComps);
            }
        } catch (Exception e) {
            // Si DataManager aún no existe o falla, no rompemos la UI, solo avisamos en consola
            System.out.println("Nota: No se pudieron cargar componentes guardados (¿DataManager listo?)");
        }

        // --- 5. AGREGAR TODO AL CONTENEDOR ---
        container.getChildren().addAll(title, btnCube, btnSphere, btnCylinder, btnRamp);
        container.getChildren().addAll(lblComp, btnNewComp, componentsList);
    }

    private Button createButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    public TitledPane getView() {
        TitledPane pane = new TitledPane("Catálogo", container);
        pane.setExpanded(true);
        return pane;
    }

    private void handleNewComponent() {
        ComponentCreationDialog dialog = new ComponentCreationDialog();
        java.util.Optional<ElectronicComponent> result = dialog.showAndWait();

        result.ifPresent(component -> {
            // 1. Obtenemos la ruta original que seleccionó el usuario (antes de guardarlo)
            String originalPath = component.getModelPath();

            // 2. Guardamos en la "Base de Datos" (JSON + Copia de Archivo)
            // DataManager se encarga de mover el archivo y actualizar la ruta en el objeto
            uv.simuladoraleexplorador.main.utils.DataManager.saveComponent(component, originalPath);

            // 3. Lo añadimos a la lista visual
            componentsList.getItems().add(component);

            System.out.println("Componente guardado y persistido: " + component.getName());
        });
    }

    private void confirmAndDelete(ElectronicComponent component) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Componente");
        alert.setHeaderText("¿Estás seguro de eliminar '" + component.getName() + "'?");
        alert.setContentText("Esta acción no se puede deshacer.");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // 1. Borrar de la "Base de Datos" (Disco)
                uv.simuladoraleexplorador.main.utils.DataManager.deleteComponent(component);

                // 2. Borrar de la Lista Visual (UI)
                componentsList.getItems().remove(component);

                System.out.println("Componente eliminado: " + component.getName());
            }
        });
    }
}