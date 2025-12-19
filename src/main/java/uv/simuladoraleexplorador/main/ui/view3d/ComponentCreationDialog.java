package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser; // Importante para abrir archivos
import uv.simuladoraleexplorador.main.model.components.ComponentType;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ComponentCreationDialog extends Dialog<ElectronicComponent> {

    private final TextField txtMass = new TextField();
    private final TextField txtName = new TextField();
    private final ComboBox<ComponentType> cmbType = new ComboBox<>();

    // --- NUEVO: SELECCIÓN DE MODELO 3D ---
    private final TextField txtModelPath = new TextField();
    private final Button btnBrowse = new Button("Examinar...");

    private final VBox dynamicContainer = new VBox(10);
    private final Map<String, TextField> dynamicFields = new HashMap<>();

    public ComponentCreationDialog() {
        this.setTitle("Nuevo Componente Electrónico");
        this.setHeaderText("Define especificaciones y modelo 3D");

        ButtonType btnCrear = new ButtonType("Guardar en BD", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(btnCrear, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        // Configuración básica
        txtName.setPromptText("Ej: Motor Llantas Traseras");
        txtMass.setPromptText("Ej: 0.5");
        cmbType.getItems().addAll(ComponentType.values());
        cmbType.setPromptText("Selecciona Tipo...");

        // Configuración del selector de archivos
        txtModelPath.setPromptText("Ruta del archivo .obj (Opcional)");
        txtModelPath.setEditable(false); // Solo lectura para evitar errores de dedo
        btnBrowse.setOnAction(e -> handleBrowseFile());

        // --- ARMADO DEL LAYOUT ---
        // Fila 0: Nombre
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtName, 1, 0);

        // Fila 1: Tipo
        grid.add(new Label("Tipo:"), 0, 1);
        grid.add(cmbType, 1, 1);

        // Fila 2: Peso (NUEVO)
        grid.add(new Label("Masa (kg):"), 0, 2);
        grid.add(txtMass, 1, 2);

        // Fila 3: Modelo 3D (Movemos esto hacia abajo)
        grid.add(new Label("Modelo 3D (.obj):"), 0, 3);
        HBox fileBox = new HBox(5, txtModelPath, btnBrowse);
        grid.add(fileBox, 1, 3);

        // Fila 4: Separador
        grid.add(new Separator(), 0, 4, 2, 1);

        // Fila 5: Dinámicos
        grid.add(new Label("Especificaciones:"), 0, 5);
        grid.add(dynamicContainer, 0, 6, 2, 1);

        this.getDialogPane().setContent(grid);
        cmbType.setOnAction(e -> rebuildDynamicFields());

        this.setResultConverter(dialogButton -> {
            if (dialogButton == btnCrear) { // btnCrear debe ser la referencia a tu ButtonType OK
                return buildComponent();
            }
            return null;
        });
    }

    private void handleBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Modelo 3D");

        // Filtro para solo ver archivos OBJ
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Modelos OBJ", "*.obj")
        );

        // Abrir diálogo
        File selectedFile = fileChooser.showOpenDialog(this.getDialogPane().getScene().getWindow());

        if (selectedFile != null) {
            txtModelPath.setText(selectedFile.getAbsolutePath());
        }
    }

    private void rebuildDynamicFields() {
        dynamicContainer.getChildren().clear();
        dynamicFields.clear();

        ComponentType selected = cmbType.getValue();
        if (selected == null) return;

        for (String propName : selected.getProperties()) {
            Label lbl = new Label(propName + ":");
            TextField txt = new TextField();
            txt.setPromptText("0.0");

            VBox fieldGroup = new VBox(2);
            fieldGroup.getChildren().addAll(lbl, txt);

            dynamicContainer.getChildren().add(fieldGroup);
            dynamicFields.put(propName, txt);
        }
        this.getDialogPane().getScene().getWindow().sizeToScene();
    }

    private ElectronicComponent buildComponent() {
        String name = txtName.getText();
        ComponentType type = cmbType.getValue();

        if (name.isEmpty() || type == null) return null;

        ElectronicComponent comp = new ElectronicComponent(name, type);

        // 1. GUARDAR MASA
        try {
            double m = Double.parseDouble(txtMass.getText());
            comp.setMass(m);
        } catch (NumberFormatException e) {
            comp.setMass(0.1); // Si escriben letras, ponemos 100g por seguridad
        }

        // 2. Guardar Ruta
        if (!txtModelPath.getText().isEmpty()) {
            comp.setModelPath(txtModelPath.getText());
        }

        // 3. Guardar Specs
        for (Map.Entry<String, TextField> entry : dynamicFields.entrySet()) {
            comp.setSpec(entry.getKey(), entry.getValue().getText());
        }

        return comp;
    }
}
