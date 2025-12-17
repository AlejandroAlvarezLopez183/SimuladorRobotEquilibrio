package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;

public class TransformGizmo extends Group {

    private final Group targetNode; // El robot al que controlamos
    private final World3D worldRef; // Referencia para avisar a la física

    // Partes del Gizmo para detectar clics
    private final Group arrowX;
    private final Group arrowY;
    private final Group arrowZ;

    // Constantes visuales
    private final double size = 20.0;
    private final double thickness = 0.8;

    // Estado del arrastre
    private double lastMouseX;
    private double lastMouseY;
    private String activeAxis = null;

    // --- CONSTRUCTOR CORREGIDO: Ahora acepta World3D ---
    public TransformGizmo(Group target, World3D worldRef) {
        this.targetNode = target;
        this.worldRef = worldRef; // Guardamos la referencia que nos pasan

        // Crear las 3 flechas
        arrowX = createArrow(Color.RED, Rotate.Z_AXIS, -90); // Rojo = X
        arrowY = createArrow(Color.GREEN, null, 0);          // Verde = Y
        arrowZ = createArrow(Color.BLUE, Rotate.X_AXIS, 90); // Azul = Z

        this.getChildren().addAll(arrowX, arrowY, arrowZ);

        // Actualizar posición inicial para pegarse al objeto
        updatePosition();
    }

    private Group createArrow(Color color, javafx.geometry.Point3D axis, double angle) {
        Group arrowGroup = new Group();
        PhongMaterial mat = new PhongMaterial(color);

        // El palo de la flecha
        Cylinder line = new Cylinder(thickness, size);
        line.setMaterial(mat);
        line.setTranslateY(size / 2.0);

        // La punta
        Box tip = new Box(thickness * 3, thickness * 3, thickness * 3);
        tip.setMaterial(mat);
        tip.setTranslateY(size);

        arrowGroup.getChildren().addAll(line, tip);

        // Rotar todo el grupo
        if (axis != null) {
            arrowGroup.getTransforms().add(new Rotate(angle, axis));
        } else {
            arrowGroup.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
        }

        // --- EVENTOS DEL MOUSE ---
        arrowGroup.setOnMousePressed(event -> {
            activeAxis = (color == Color.RED) ? "X" : (color == Color.GREEN) ? "Y" : "Z";
            lastMouseX = event.getSceneX();
            lastMouseY = event.getSceneY();
            event.consume();
            mat.setSpecularColor(Color.WHITE);
        });

        arrowGroup.setOnMouseReleased(event -> {
            activeAxis = null;
            mat.setSpecularColor(null);
        });

        arrowGroup.setOnMouseDragged(event -> {
            if (activeAxis != null) {
                double dx = event.getSceneX() - lastMouseX;
                double dy = event.getSceneY() - lastMouseY;

                double speed = 0.5;

                // Mover el objeto TARGET
                if (activeAxis.equals("X")) {
                    targetNode.setTranslateX(targetNode.getTranslateX() + (dx * speed));
                } else if (activeAxis.equals("Y")) {
                    targetNode.setTranslateY(targetNode.getTranslateY() + (dy * speed));
                } else if (activeAxis.equals("Z")) {
                    targetNode.setTranslateZ(targetNode.getTranslateZ() - (dy * speed));
                }

                // 1. Mover visualmente el Gizmo
                updatePosition();

                // 2. AVISAR A LA FÍSICA (Esto conecta con PhysicsEngine)
                worldRef.notifyObjectMovedManually(targetNode);

                lastMouseX = event.getSceneX();
                lastMouseY = event.getSceneY();
                event.consume();
            }
        });

        return arrowGroup;
    }

    public void updatePosition() {
        this.setTranslateX(targetNode.getTranslateX());
        this.setTranslateY(targetNode.getTranslateY());
        this.setTranslateZ(targetNode.getTranslateZ());
    }
    public Group getTargetNode() {
        return targetNode;
    }
}