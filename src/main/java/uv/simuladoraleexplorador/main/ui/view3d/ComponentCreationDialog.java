package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import uv.simuladoraleexplorador.main.model.components.ComponentType;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ComponentCreationDialog extends Dialog<ElectronicComponent> {

    private final TextField txtName = new TextField();
    private final ComboBox<ComponentType> cmbType = new ComboBox<>();
    private final VBox dynamicContainer = new VBox(10); // Aquí pondremos los campos variables

    // Mapa para guardar referencias a los TextFields generados dinámicamente
    private final Map<String, TextField> dynamicFields = new HashMap<>();

    public ComponentCreationDialog() {
        this.setTitle("Nuevo Componente Electrónico");
        this.setHeaderText("Define las especificaciones técnicas");

        // Botones
        ButtonType btnCrear = new ButtonType("Guardar en BD", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(btnCrear, ButtonType.CANCEL);

        // Layout Principal
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Campos Fijos
        txtName.setPromptText("Ej: Motor Llantas Traseras");
        cmbType.getItems().addAll(ComponentType.values());
        cmbType.setPromptText("Selecciona Tipo...");

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Tipo:"), 0, 1);
        grid.add(cmbType, 1, 1);

        // Contenedor para los campos que cambian
        grid.add(dynamicContainer, 0, 2, 2, 1);

        this.getDialogPane().setContent(grid);

        // --- LÓGICA DINÁMICA ---
        // Cuando cambie el Combo, reconstruimos el formulario
        cmbType.setOnAction(e -> rebuildDynamicFields());

        // Convertir el resultado al cerrar
        this.setResultConverter(dialogButton -> {
            if (dialogButton == btnCrear) {
                return buildComponent();
            }
            return null;
        });
    }

    private void rebuildDynamicFields() {
        dynamicContainer.getChildren().clear();
        dynamicFields.clear();

        ComponentType selected = cmbType.getValue();
        if (selected == null) return;

        // Recorremos las propiedades definidas en el Enum
        for (String propName : selected.getProperties()) {
            Label lbl = new Label(propName + ":");
            TextField txt = new TextField();
            txt.setPromptText("Ingresa valor...");

            // Un poco de estilo
            VBox fieldGroup = new VBox(2);
            fieldGroup.getChildren().addAll(lbl, txt);

            dynamicContainer.getChildren().add(fieldGroup);
            dynamicFields.put(propName, txt);
        }

        // Redimensionar ventana automáticamente
        this.getDialogPane().getScene().getWindow().sizeToScene();
    }

    private ElectronicComponent buildComponent() {
        String name = txtName.getText();
        ComponentType type = cmbType.getValue();

        if (name.isEmpty() || type == null) return null;

        ElectronicComponent comp = new ElectronicComponent(name, type);

        // Guardamos lo que el usuario escribió en cada campo dinámico
        for (Map.Entry<String, TextField> entry : dynamicFields.entrySet()) {
            comp.setSpec(entry.getKey(), entry.getValue().getText());
        }

        return comp;
    }
}