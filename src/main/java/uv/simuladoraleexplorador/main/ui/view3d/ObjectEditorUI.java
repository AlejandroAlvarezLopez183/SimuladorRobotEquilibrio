package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.Group;
import com.bulletphysics.dynamics.RigidBody;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;

public class ObjectEditorUI {

    private final VBox container;
    private final PhysicsEngine physics;

    // Referencia al objeto seleccionado actualmente
    private Node currentNode;
    private RigidBody currentBody;
    private RobotManager.ShapeType currentType;

    public ObjectEditorUI(PhysicsEngine physics) {
        this.physics = physics;
        this.container = new VBox(10);
        this.container.setPadding(new Insets(10));
        this.container.setStyle("-fx-background-color: #2b2b2b;");

        // Mensaje por defecto
        Label lbl = new Label("Selecciona un objeto para editar");
        lbl.setStyle("-fx-text-fill: #888;");
        container.getChildren().add(lbl);
    }

    public TitledPane getView() {
        TitledPane pane = new TitledPane("Editor de Forma", container);
        pane.setExpanded(true);
        return pane;
    }

    /**
     * Este método se llamará cuando hagas clic en un objeto del mundo 3D.
     */
    public void setSelectedObject(Node node, RigidBody body, RobotManager.ShapeType type) {
        this.currentNode = node;
        this.currentBody = body;
        this.currentType = type;

        refreshUI();
    }

    private void refreshUI() {
        container.getChildren().clear();

        if (currentNode == null || currentType == null) {
            container.getChildren().add(new Label("Nada seleccionado"));
            return;
        }

        switch (currentType) {
            case BOX:
                buildBoxControls();
                break;
            case SPHERE:
                buildSphereControls();
                break;
            case CYLINDER:
                buildCylinderControls();
                break;
            case RAMP:
            default:
                container.getChildren().add(new Label("Este objeto no es editable (aún)."));
                break;
        }
    }

    // --- CONSTRUCTORES DE PANELES ESPECÍFICOS ---

    private void buildBoxControls() {
        // Intento 1: ¿Es una Caja Primitiva (creada por nosotros)?
        Box boxGraphic = findShapeInGroup(currentNode, Box.class);

        if (boxGraphic != null) {
            // Lógica para Caja Primitiva: Sliders individuales de tamaño
            addSlider("Ancho (X)", 1, 50, boxGraphic.getWidth(), val -> {
                boxGraphic.setWidth(val); // Visual
                physics.resizeBoxBody(currentBody, (float)val, (float)boxGraphic.getHeight(), (float)boxGraphic.getDepth()); // Físico
            });

            addSlider("Alto (Y)", 1, 50, boxGraphic.getHeight(), val -> {
                boxGraphic.setHeight(val); // Visual
                physics.resizeBoxBody(currentBody, (float)boxGraphic.getWidth(), (float)val, (float)boxGraphic.getDepth()); // Físico
            });

            addSlider("Profundidad (Z)", 1, 50, boxGraphic.getDepth(), val -> {
                boxGraphic.setDepth(val); // Visual
                physics.resizeBoxBody(currentBody, (float)boxGraphic.getWidth(), (float)boxGraphic.getHeight(), (float)val); // Físico
            });
        } else {
            // Intento 2: Es un Modelo Importado (Mesa, Robot, etc.) -> Usamos ESCALAS INDIVIDUALES
            // Nota: Al escalar un modelo importado, asumimos que su collider es una CAJA

            // Dimensiones originales sin escala (usamos getBoundsInLocal)
            javafx.geometry.Bounds originalBounds = currentNode.getBoundsInLocal();
            double originalW = originalBounds.getWidth();
            double originalH = originalBounds.getHeight();
            double originalD = originalBounds.getDepth();

            // Slider Escala X (Ancho)
            addSlider("Escala X (Ancho)", 0.1, 5.0, currentNode.getScaleX(), val -> {
                currentNode.setScaleX(val); // Escala visual
                updatePhysicsForImportedObject(originalW, originalH, originalD); // Escala física
            });

            // Slider Escala Y (Alto)
            addSlider("Escala Y (Alto)", 0.1, 5.0, currentNode.getScaleY(), val -> {
                currentNode.setScaleY(val); // Escala visual
                updatePhysicsForImportedObject(originalW, originalH, originalD); // Escala física
            });

            // Slider Escala Z (Profundidad)
            addSlider("Escala Z (Profundidad)", 0.1, 5.0, currentNode.getScaleZ(), val -> {
                currentNode.setScaleZ(val); // Escala visual
                updatePhysicsForImportedObject(originalW, originalH, originalD); // Escala física
            });

            Label nota = new Label("(Objeto Importado: Edición por Escala Individual)");
            nota.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
            container.getChildren().add(nota);
        }
    }

    private void buildSphereControls() {
        Sphere sphereGraphic = findShapeInGroup(currentNode, Sphere.class);
        if (sphereGraphic == null) return;

        addSlider("Radio", 1, 30, sphereGraphic.getRadius(), val -> {
            sphereGraphic.setRadius(val); // Visual
            physics.resizeSphereBody(currentBody, (float)val); // Físico
        });
    }

    private void buildCylinderControls() {
        Cylinder cylGraphic = findShapeInGroup(currentNode, Cylinder.class);
        if (cylGraphic == null) return;

        addSlider("Radio", 1, 20, cylGraphic.getRadius(), val -> {
            cylGraphic.setRadius(val); // Visual
            physics.resizeCylinderBody(currentBody, (float)val, (float)cylGraphic.getHeight()); // Físico
        });

        addSlider("Altura", 1, 60, cylGraphic.getHeight(), val -> {
            cylGraphic.setHeight(val); // Visual
            physics.resizeCylinderBody(currentBody, (float)cylGraphic.getRadius(), (float)val); // Físico
        });
    }

    // --- UTILIDADES ---

    private void addSlider(String name, double min, double max, double current, java.util.function.DoubleConsumer action) {
        Label lbl = new Label(name);
        lbl.setStyle("-fx-text-fill: #ccc;");

        Slider slider = new Slider(min, max, current);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);

        // Listener: Actualizar en tiempo real al arrastrar
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            action.accept(newVal.doubleValue());
        });

        container.getChildren().addAll(lbl, slider);
    }

    // Método auxiliar para actualizar la física del objeto importado
    private void updatePhysicsForImportedObject(double originalW, double originalH, double originalD) {
        double scaleX = currentNode.getScaleX();
        double scaleY = currentNode.getScaleY();
        double scaleZ = currentNode.getScaleZ();

        float newW = (float) (originalW * scaleX);
        float newH = (float) (originalH * scaleY);
        float newD = (float) (originalD * scaleZ);

        // Actualizar física
        physics.resizeBoxBody(currentBody, newW, newH, newD);
    }

    // Busca la forma geométrica (Box, Sphere) dentro del Grupo del nodo
    private <T extends Node> T findShapeInGroup(Node node, Class<T> clazz) {
        if (clazz.isInstance(node)) return clazz.cast(node);
        if (node instanceof Group) {
            for (Node child : ((Group) node).getChildren()) {
                if (clazz.isInstance(child)) return clazz.cast(child);
            }
        }
        return null;
    }
}