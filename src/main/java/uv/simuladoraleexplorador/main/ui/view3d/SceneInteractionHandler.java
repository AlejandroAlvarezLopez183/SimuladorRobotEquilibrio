package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import com.bulletphysics.dynamics.RigidBody;
import java.util.ArrayList;
import java.util.List;

public class SceneInteractionHandler {

    private final World3D world3D;
    private final ObjectEditorUI objectEditor;
    private final Rectangle selectionRect;
    private final ComboBox<Double> cmbSnap;

    private double lastMouseX, lastMouseY;
    private double startSelX, startSelY;
    private boolean isDraggingObject = false;
    private boolean isBoxSelecting = false;

    private final List<Node> multiSelection = new ArrayList<>();

    public SceneInteractionHandler(World3D world3D, ObjectEditorUI objectEditor, Rectangle selectionRect, ComboBox<Double> cmbSnap) {
        this.world3D = world3D;
        this.objectEditor = objectEditor;
        this.selectionRect = selectionRect;
        this.cmbSnap = cmbSnap;
        initEvents();
    }

    private void initEvents() {
        world3D.getSubScene().addEventHandler(MouseEvent.MOUSE_PRESSED, this::handlePressed);
        world3D.getSubScene().addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleDragged);
        world3D.getSubScene().addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleReleased);
    }

    private void handlePressed(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) return;

        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();

        Node pickedNode = event.getPickResult().getIntersectedNode();
        Node rootObj = findRootObject(pickedNode);

        // 1. GIZMO
        if (isGizmoPart(pickedNode)) {
            isDraggingObject = false;
            isBoxSelecting = false;
            return;
        }

        // 2. OBJETO (Ahora soporta GRUPOS)
        if (rootObj != null) {
            isDraggingObject = true;
            isBoxSelecting = false;

            RigidBody body = world3D.getPhysicsEngine().getBodyFromGraphic(rootObj);
            Object userData = rootObj.getUserData();

            if (body != null) {
                // --- CORRECCIÓN AQUÍ ---
                // Determinar el tipo de forma de manera segura
                RobotManager.ShapeType shapeType = null;

                if (userData instanceof RobotManager.ShapeType) {
                    shapeType = (RobotManager.ShapeType) userData;
                } else if ("GRUPO".equals(userData)) {
                    // Si es un grupo, pasamos null o BOX como dummy,
                    // ya que ObjectEditorUI ahora detecta "GRUPO" por su cuenta.
                    shapeType = RobotManager.ShapeType.BOX;
                }

                objectEditor.setSelectedObject(rootObj, body, shapeType);

                // Multiselección
                if (!event.isControlDown()) {
                    multiSelection.clear();
                    multiSelection.add(rootObj);
                } else if (!multiSelection.contains(rootObj)) {
                    multiSelection.add(rootObj);
                }

                // Pausar física al agarrar
                body.setLinearVelocity(new javax.vecmath.Vector3f(0,0,0));
                body.setAngularVelocity(new javax.vecmath.Vector3f(0,0,0));
                body.activate();
            }
            event.consume();
        }
        // 3. VACÍO (Caja de Selección)
        else {
            isDraggingObject = false;
            isBoxSelecting = true;

            Point2D localPoint = selectionRect.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
            startSelX = localPoint.getX();
            startSelY = localPoint.getY();

            setupRect(startSelX, startSelY, 0, 0, true);

            if (!event.isControlDown()) {
                objectEditor.setSelectedObject(null, null, null);
                multiSelection.clear();
            }
        }
    }

    private void handleDragged(MouseEvent event) {
        if (!event.isPrimaryButtonDown()) return;

        if (isDraggingObject) {
            moveSelectedObject(event);
        } else if (isBoxSelecting) {
            updateSelectionBox(event);
        }
    }

    private void handleReleased(MouseEvent event) {
        if (isBoxSelecting) {
            performBoxSelection();
            selectionRect.setVisible(false);
            isBoxSelecting = false;
        }
        if (isDraggingObject) {
            RigidBody body = objectEditor.getCurrentBody();
            if(body != null) body.activate();
        }
        isDraggingObject = false;
    }

    private void moveSelectedObject(MouseEvent event) {
        Node selectedNode = objectEditor.getCurrentNode();
        if (selectedNode == null) return;

        double mouseDx = event.getSceneX() - lastMouseX;
        double mouseDy = event.getSceneY() - lastMouseY;

        // Movimiento Vertical con SHIFT (La mejora que te sugerí antes)
        if (event.isShiftDown()) {
            double moveY = -mouseDy * 0.1;
            selectedNode.setTranslateY(selectedNode.getTranslateY() + moveY);
        } else {
            // Movimiento Horizontal Normal
            double camAngle = Math.toRadians(world3D.getCameraAngleY());
            double sens = 0.5;

            double moveX = (mouseDx * Math.cos(camAngle)) - (mouseDy * Math.sin(camAngle));
            double moveZ = (mouseDx * Math.sin(camAngle)) + (mouseDy * Math.cos(camAngle));

            double rawX = selectedNode.getTranslateX() + (moveX * sens);
            double rawZ = selectedNode.getTranslateZ() + (moveZ * sens);

            Double snap = (cmbSnap != null) ? cmbSnap.getValue() : null;
            if (snap != null && snap > 0.0) {
                rawX = Math.round(rawX / snap) * snap;
                rawZ = Math.round(rawZ / snap) * snap;
            }

            selectedNode.setTranslateX(rawX);
            selectedNode.setTranslateZ(rawZ);
        }

        world3D.notifyObjectMovedManually(selectedNode);

        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();
    }

    private void updateSelectionBox(MouseEvent event) {
        Point2D currentLocal = selectionRect.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
        double currentX = currentLocal.getX();
        double currentY = currentLocal.getY();
        double x = Math.min(startSelX, currentX);
        double y = Math.min(startSelY, currentY);
        double w = Math.abs(currentX - startSelX);
        double h = Math.abs(currentY - startSelY);
        setupRect(x, y, w, h, true);
    }

    private void setupRect(double x, double y, double w, double h, boolean visible) {
        selectionRect.setX(x); selectionRect.setY(y);
        selectionRect.setWidth(w); selectionRect.setHeight(h);
        selectionRect.setVisible(visible);
    }

    private void performBoxSelection() {
        List<Node> candidates = world3D.getRobotManager().getTrackedNodes();
        Bounds rectBounds = selectionRect.localToScene(selectionRect.getBoundsInLocal());

        for (Node node : candidates) {
            javafx.geometry.Point3D posInScene3D = node.localToScene(0, 0, 0);
            javafx.geometry.Point3D posInScreen = world3D.getSubScene().localToScene(posInScene3D);

            if (rectBounds.contains(posInScreen.getX(), posInScreen.getY())) {
                if (!multiSelection.contains(node)) {
                    multiSelection.add(node);
                }
            }
        }

        if (!multiSelection.isEmpty()) {
            Node last = multiSelection.get(multiSelection.size() - 1);
            RigidBody b = world3D.getPhysicsEngine().getBodyFromGraphic(last);
            Object userData = last.getUserData();

            // Lógica corregida también para selección de caja
            RobotManager.ShapeType type = RobotManager.ShapeType.BOX;
            if (userData instanceof RobotManager.ShapeType) type = (RobotManager.ShapeType) userData;

            if (b != null) {
                objectEditor.setSelectedObject(last, b, type);
            }
        }
    }

    // --- CORRECCIÓN CLAVE EN ESTE MÉTODO ---
    private Node findRootObject(Node node) {
        while (node != null) {
            Object data = node.getUserData();
            // AHORA ACEPTAMOS 'GRUPO' COMO VÁLIDO
            if (data instanceof RobotManager.ShapeType || "GRUPO".equals(data)) {
                return node;
            }
            node = node.getParent();
        }
        return null;
    }

    private boolean isGizmoPart(Node node) {
        while (node != null) {
            if (node instanceof TransformGizmo) return true;
            node = node.getParent();
        }
        return false;
    }

    // Getters
    public List<Node> getMultiSelection() { return multiSelection; }
    public void clearSelection() { multiSelection.clear(); }
}