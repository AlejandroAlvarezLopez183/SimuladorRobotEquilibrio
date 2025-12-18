package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.Group;
import com.bulletphysics.dynamics.RigidBody;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;

public class ObjectEditorUI {

    private final VBox container;
    private final PhysicsEngine physics;

    public Node getCurrentNode() { return currentNode; }
    public RigidBody getCurrentBody() { return currentBody; }
    private Box selectionBoxIndicator = null;
    // Estado actual
    private Node currentNode;
    private RigidBody currentBody;
    private RobotManager.ShapeType currentType;
    private final ObjectProperty<Node> selectedNodeProperty = new SimpleObjectProperty<>(null);
    private PhongMaterial lastMaterial;
    private Shape3D lastShape;

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

    public void setSelectedObject(Node node, RigidBody body, RobotManager.ShapeType type) {
        // 1. Restaurar el objeto anterior si existía
        restorePreviousEffect();

        this.currentNode = node;
        this.currentBody = body;
        this.currentType = type;
        selectedNodeProperty.set(node);

        applySelectionEffect(node);

        refreshUI();
    }
    public ObjectProperty<Node> selectedNodeProperty() {
        return selectedNodeProperty;
    }
    private void applySelectionEffect(Node node) {
        // 1. Limpiar indicador anterior
        if (selectionBoxIndicator != null && selectionBoxIndicator.getParent() != null) {
            ((Group)selectionBoxIndicator.getParent()).getChildren().remove(selectionBoxIndicator);
            selectionBoxIndicator = null;
        }

        if (node instanceof Group) {
            Group group = (Group) node;

            // 2. Calcular tamaño visual (Bounds)
            javafx.geometry.Bounds b = group.getBoundsInLocal();

            // 3. Crear caja de alambre (Wireframe)
            selectionBoxIndicator = new Box(b.getWidth() + 0.2, b.getHeight() + 0.2, b.getDepth() + 0.2);
            selectionBoxIndicator.setDrawMode(javafx.scene.shape.DrawMode.LINE); // Solo líneas
            selectionBoxIndicator.setMaterial(new PhongMaterial(Color.CYAN));
            selectionBoxIndicator.setMouseTransparent(true); // Para no interferir con clics

            // 4. Añadir como hijo del objeto seleccionado
            group.getChildren().add(selectionBoxIndicator);
        }
    }

    private void refreshUI() {
        container.getChildren().clear();

        if (currentNode == null || currentType == null) {
            container.getChildren().add(new Label("Nada seleccionado"));
            return;
        }

        // Switch para decidir qué controles mostrar
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
            case RAMP: // ¡AHORA SÍ SOPORTAMOS LA RAMPA!
                buildRampControls();
                break;
            default:
                container.getChildren().add(new Label("Objeto no editable."));
                break;
        }
    }

    // --- PANELES ESPECÍFICOS ---

    private void buildBoxControls() {
        // Buscamos la forma visual exacta (La caja naranja, no la roja de debug)
        Box boxGraphic = findShapeInGroup(currentNode, Box.class);

        if (boxGraphic != null && isNotDebugBox(boxGraphic)) {
            // ES UN CUBO NATIVO (Tiene forma Box visual)
            addSlider("Ancho (X)", 1, 50, boxGraphic.getWidth(), val -> {
                boxGraphic.setWidth(val);
                physics.resizeBoxBody(currentBody, (float)val, (float)boxGraphic.getHeight(), (float)boxGraphic.getDepth());
            });
            addSlider("Alto (Y)", 1, 50, boxGraphic.getHeight(), val -> {
                boxGraphic.setHeight(val);
                physics.resizeBoxBody(currentBody, (float)boxGraphic.getWidth(), (float)val, (float)boxGraphic.getDepth());
            });
            addSlider("Profundidad (Z)", 1, 50, boxGraphic.getDepth(), val -> {
                boxGraphic.setDepth(val);
                physics.resizeBoxBody(currentBody, (float)boxGraphic.getWidth(), (float)boxGraphic.getHeight(), (float)val);
            });
        } else {
            // ES UN OBJETO IMPORTADO (Mesa, Silla...) -> Usamos Escala
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

    // --- REUTILIZABLE: Controles de Escala (X, Y, Z) ---
    // Sirve para Rampa y Objetos Importados
    private void buildScalingControls(String labelTitle) {
        // Importante: Usamos la escala del NODO PADRE (currentNode)
        // Esto escala todo: el gráfico visual y le decimos a físicas que escale la colisión.

        addSlider("Escala X (Ancho)", 0.1, 5.0, currentNode.getScaleX(), val -> {
            currentNode.setScaleX(val);
            physics.updateBodyScale(currentBody, (float)val, (float)currentNode.getScaleY(), (float)currentNode.getScaleZ());
        });

        addSlider("Escala Y (Alto)", 0.1, 5.0, currentNode.getScaleY(), val -> {
            currentNode.setScaleY(val);
            physics.updateBodyScale(currentBody, (float)currentNode.getScaleX(), (float)val, (float)currentNode.getScaleZ());
        });

        addSlider("Escala Z (Largo)", 0.1, 5.0, currentNode.getScaleZ(), val -> {
            currentNode.setScaleZ(val);
            physics.updateBodyScale(currentBody, (float)currentNode.getScaleX(), (float)currentNode.getScaleY(), (float)val);
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

    private void restorePreviousEffect() {
        if (lastShape != null && lastMaterial != null) {
            lastShape.setMaterial(lastMaterial);
            lastShape = null;
            lastMaterial = null;
        }
    }
}