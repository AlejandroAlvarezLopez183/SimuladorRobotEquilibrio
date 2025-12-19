package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    public Node getCurrentNode() { return currentNode; }
    public RigidBody getCurrentBody() { return currentBody; }
    private final SelectionVisualizer visualizer = new SelectionVisualizer();
    // Estado actual
    private Node currentNode;
    private RigidBody currentBody;
    private RobotManager.ShapeType currentType;
    private final ObjectProperty<Node> selectedNodeProperty = new SimpleObjectProperty<>(null);

    public ObjectEditorUI(PhysicsEngine physics) {
        this.physics = physics;
        this.container = new VBox(10);
        this.container.setPadding(new Insets(10));
        this.container.setStyle("-fx-background-color: #2b2b2b;");

        Label lbl = new Label("Selecciona un objeto para editar");
        lbl.setStyle("-fx-text-fill: #888;");
        container.getChildren().add(lbl);
    }

    public TitledPane getView() {
        TitledPane pane = new TitledPane("Editor de Forma", container);
        pane.setExpanded(true);
        return pane;
    }

    public ObjectProperty<Node> selectedNodeProperty() {
        return selectedNodeProperty;
    }

    private void refreshUI() {
        container.getChildren().clear();

        if (currentNode == null) {
            container.getChildren().add(new Label("Nada seleccionado"));
            return;
        }

        // --- CORRECCIÓN LÓGICA ---
        // Detectar si es un Grupo Compuesto
        boolean esGrupo = "GRUPO".equals(currentNode.getUserData()) ||
                (currentNode instanceof Group && ((Group)currentNode).getChildren().size() > 1);

        if (esGrupo) {
            // Si es un grupo, mostramos ESCALA GLOBAL
            buildScalingControls("Grupo de Objetos");
            return; // ¡Importante! Salimos aquí para no dibujar los controles de Caja/Esfera
        }

        // Si no es grupo, seguimos con la lógica normal...
        if (currentType == null) return;

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
                buildScalingControls("Rampa"); // La rampa usa escala
                break;
        }
    }

    // --- PANELES ESPECÍFICOS ---

    private void buildBoxControls() {
        Box boxGraphic = findShapeInGroup(currentNode, Box.class);

        if (boxGraphic != null && isNotDebugBox(boxGraphic)) {
            addSlider("Ancho (X)", 1, 50, boxGraphic.getWidth(), val -> {
                boxGraphic.setWidth(val);
                physics.resizeBoxBody(currentBody, (float)val, (float)boxGraphic.getHeight(), (float)boxGraphic.getDepth());
                preventFloorPenetration(); // <--- AGREGAR
            });

            addSlider("Alto (Y)", 1, 50, boxGraphic.getHeight(), val -> {
                boxGraphic.setHeight(val);
                physics.resizeBoxBody(currentBody, (float)boxGraphic.getWidth(), (float)val, (float)boxGraphic.getDepth());
                preventFloorPenetration(); // <--- AGREGAR (Vital para altura)
            });

            addSlider("Profundidad (Z)", 1, 50, boxGraphic.getDepth(), val -> {
                boxGraphic.setDepth(val);
                physics.resizeBoxBody(currentBody, (float)boxGraphic.getWidth(), (float)boxGraphic.getHeight(), (float)val);
                preventFloorPenetration(); // <--- AGREGAR
            });
        } else {
            buildScalingControls("Objeto Importado");
        }
    }
    private void buildSphereControls() {
        // Búsqueda recursiva encuentra la esfera aunque esté muy anidada
        Sphere sphereGraphic = findShapeInGroup(currentNode, Sphere.class);

        if (sphereGraphic != null) {
            addSlider("Radio", 1, 30, sphereGraphic.getRadius(), val -> {
                sphereGraphic.setRadius(val);
                physics.resizeSphereBody(currentBody, (float)val);
            });
        } else {
            container.getChildren().add(new Label("Error: No encuentro la forma visual Sphere."));
        }
    }

    private void buildCylinderControls() {
        Cylinder cylGraphic = findShapeInGroup(currentNode, Cylinder.class);

        if (cylGraphic != null) {
            addSlider("Radio", 1, 20, cylGraphic.getRadius(), val -> {
                cylGraphic.setRadius(val);
                physics.resizeCylinderBody(currentBody, (float)val, (float)cylGraphic.getHeight());
            });
            addSlider("Altura", 1, 60, cylGraphic.getHeight(), val -> {
                cylGraphic.setHeight(val);
                physics.resizeCylinderBody(currentBody, (float)cylGraphic.getRadius(), (float)val);
            });
        } else {
            container.getChildren().add(new Label("Error: No encuentro la forma visual Cylinder."));
        }
    }

    private void buildRampControls() {
        // Para la rampa, usamos los controles de Escala, ya que es una forma compleja
        buildScalingControls("Rampa (Escala)");
    }

    private void buildScalingControls(String labelTitle) {
        // Guardamos la referencia para no buscarla mil veces
        RigidBody body = currentBody;
        Node node = currentNode;

        addSlider("Escala X (Ancho)", 0.1, 5.0, node.getScaleX(), val -> {
            node.setScaleX(val);
            updatePhysicsAndVisuals(node, body);
        });

        addSlider("Escala Y (Alto)", 0.1, 5.0, node.getScaleY(), val -> {
            // 1. Guardamos dónde están los pies ANTES de escalar
            double oldBottomY = node.getBoundsInParent().getMaxY();

            // 2. Aplicamos la escala
            node.setScaleY(val);

            // 3. Calculamos dónde quedaron los pies AHORA
            double newBottomY = node.getBoundsInParent().getMaxY();

            // 4. La diferencia es lo que se enterró en el suelo
            double difference = newBottomY - oldBottomY;

            // 5. Lo subimos esa diferencia exacta para que parezca que crece hacia arriba
            node.setTranslateY(node.getTranslateY() - difference);

            updatePhysicsAndVisuals(node, body);
        });

        addSlider("Escala Z (Largo)", 0.1, 5.0, node.getScaleZ(), val -> {
            node.setScaleZ(val);
            updatePhysicsAndVisuals(node, body);
        });

        Label lbl = new Label("(" + labelTitle + ")");
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        container.getChildren().add(lbl);
    }

    // --- UTILIDADES ---

    private void addSlider(String name, double min, double max, double current, java.util.function.DoubleConsumer action) {
        Label lbl = new Label(name);
        lbl.setStyle("-fx-text-fill: #ccc;");
        Slider slider = new Slider(min, max, current);
        slider.valueProperty().addListener((obs, old, val) -> action.accept(val.doubleValue()));
        container.getChildren().addAll(lbl, slider);
    }

    // --- CORRECCIÓN CLAVE: BÚSQUEDA RECURSIVA ---
    // Esto arregla el problema de que no encontraba Cilindros ni Esferas
    private <T extends Node> T findShapeInGroup(Node node, Class<T> clazz) {
        // 1. ¿Es este nodo lo que busco?
        if (clazz.isInstance(node)) return clazz.cast(node);

        // 2. Si es un grupo, buscar en sus hijos (¡Y en los hijos de sus hijos!)
        if (node instanceof Group) {
            for (Node child : ((Group) node).getChildren()) {
                T found = findShapeInGroup(child, clazz); // Llamada recursiva
                if (found != null) return found;
            }
        }
        return null;
    }

    // Pequeño filtro para evitar editar la "Caja Roja" de debug si llegara a aparecer
    private boolean isNotDebugBox(Box box) {
        // Las cajas de debug son transparentes al mouse o DrawMode.LINE
        return !box.isMouseTransparent();
    }
    public void setSelectedObject(Node node, RigidBody body, RobotManager.ShapeType type) {
        visualizer.detach(); // Limpiar anterior

        this.currentNode = node;
        this.currentBody = body;
        this.currentType = type;
        selectedNodeProperty.set(node);

        if (node != null) {
            visualizer.attach(node); // Poner nuevo
        }
        refreshUI();
    }
    private void preventFloorPenetration() {
        if (currentNode == null) return;

        javafx.geometry.Bounds b = currentNode.getBoundsInParent();

        // El suelo físico está en 0.0. Usamos un margen de error pequeñito (0.01)
        if (b.getMaxY() > 0.01) {
            double penetration = b.getMaxY();
            // Subimos el objeto
            currentNode.setTranslateY(currentNode.getTranslateY() - penetration);
        }
    }
    private void updatePhysicsAndVisuals(Node node, RigidBody body) {
        physics.updateBodyScale(body, (float)node.getScaleX(), (float)node.getScaleY(), (float)node.getScaleZ());
        preventFloorPenetration();
        physics.updatePhysicsFromGraphicPosition(node);

        visualizer.update(node); // ¡Mucho más legible!
    }
}