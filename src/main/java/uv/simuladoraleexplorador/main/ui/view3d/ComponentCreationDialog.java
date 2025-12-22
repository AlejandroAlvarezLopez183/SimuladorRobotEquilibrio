package uv.simuladoraleexplorador.main.ui.view3d;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser; // Importante para abrir archivos
import uv.simuladoraleexplorador.main.model.components.ComponentType;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;
import uv.simuladoraleexplorador.main.model.components.data.MicrocontrollerData; // La clase de datos que hicimos antes
import java.io.File;
import java.io.FileWriter;
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

        if (name.isEmpty() || type == null) {
            new Alert(Alert.AlertType.WARNING, "Faltan datos obligatorios").show();
            return null;
        }

        ElectronicComponent comp = new ElectronicComponent(name, type);

        // 1. MASA
        try {
            double m = Double.parseDouble(txtMass.getText());
            comp.setMass(m);
        } catch (NumberFormatException e) {
            comp.setMass(0.1);
        }

        // 2. RUTA 3D
        if (!txtModelPath.getText().isEmpty()) {
            comp.setModelPath(txtModelPath.getText());
        }

        // 3. SPECS GENERALES
        for (Map.Entry<String, TextField> entry : dynamicFields.entrySet()) {
            comp.setSpec(entry.getKey(), entry.getValue().getText());
        }

        // --- LA INTEGRACIÓN NUEVA ---
        // Si el usuario eligió MICROCONTROLLER, generamos el JSON aquí mismo
        if (type == ComponentType.MICROCONTROLLER) {
            boolean exito = saveMicrocontrollerJSON(comp);
            if (!exito) return null; // Si canceló el guardado, no cerramos el diálogo
        }

        return comp;
    }

    private boolean saveMicrocontrollerJSON(ElectronicComponent comp) {
        try {
            // 1. Recopilar datos (Igual que antes)
            double voltaje = parseDoubleSafe(dynamicFields.get("Voltaje Op (V)").getText(), 5.0);
            int digPins = parseIntSafe(dynamicFields.get("Pines Digitales").getText(), 14);
            int anaPins = parseIntSafe(dynamicFields.get("Pines Analogicos").getText(), 6);

            MicrocontrollerData data = new MicrocontrollerData();
            data.name = comp.getName();
            data.id = comp.getName().toLowerCase().replace(" ", "_"); // ID seguro
            data.modelPath = comp.getModelPath();
            data.operatingVoltage = voltaje;
            data.totalDigitalPins = digPins;
            data.totalAnalogPins = anaPins;

            // 2. Definir la CARPETA POR DEFECTO
            // Se creará una carpeta llamada "custom_components" en la raíz de tu proyecto
            File folder = new File("custom_components");
            if (!folder.exists()) {
                folder.mkdirs(); // Si no existe, la creamos
            }

            // 3. Crear el archivo directamente sin preguntar al usuario
            File dest = new File(folder, data.id + ".json");

            // 4. Guardar JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(dest)) {
                writer.write(gson.toJson(data));
                // Opcional: Avisar en consola
                System.out.println(">> Configuración guardada automáticamente en: " + dest.getPath());
                return true;
            }

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error guardando configuración: " + e.getMessage()).show();
        }
        return false;
    }

    // Helpers para evitar errores si el usuario deja campos vacíos o escribe letras
    private double parseDoubleSafe(String val, double def) {
        try { return Double.parseDouble(val); } catch (Exception e) { return def; }
    }

    private int parseIntSafe(String val, int def) {
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }

}
