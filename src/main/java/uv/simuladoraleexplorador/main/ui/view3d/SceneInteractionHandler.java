package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import uv.simuladoraleexplorador.main.ui.MainController; // O una interfaz si prefieres desacoplar más
import com.bulletphysics.dynamics.RigidBody;

import java.util.ArrayList;
import java.util.List;

public class SceneInteractionHandler {

    private final World3D world3D;
    private final ObjectEditorUI objectEditor;
    private final Rectangle selectionRect;
    private final ComboBox<Double> cmbSnap;

    // Estados internos
    private double lastMouseX, lastMouseY;
    private double startSelX, startSelY;
    private boolean isDraggingObject = false;
    private boolean isBoxSelecting = false;

    // Listas de selección
    private final List<Node> multiSelection = new ArrayList<>();

    // Referencias para agrupación temporal (A -> B)
    private Node nodeA, nodeB;
    private RigidBody bodyA, bodyB;

    public SceneInteractionHandler(World3D world3D, ObjectEditorUI objectEditor, Rectangle selectionRect, ComboBox<Double> cmbSnap) {
        this.world3D = world3D;
        this.objectEditor = objectEditor;
        this.selectionRect = selectionRect;
        this.cmbSnap = cmbSnap;

        initEvents();
    }

    private void initEvents() {
        world3D.getSubScene().addEventHandler(MouseEvent.ANY, event -> {
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED) handlePressed(event);
            else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) handleDragged(event);
            else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) handleReleased(event);
        });
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

        // 2. OBJETO
        if (rootObj != null) {
            isDraggingObject = true;
            isBoxSelecting = false;

            RigidBody body = world3D.getPhysicsEngine().getBodyFromGraphic(rootObj);
            Object typeObj = rootObj.getUserData();

            if (body != null && typeObj instanceof RobotManager.ShapeType) {
                // Lógica A/B
                nodeA = nodeB; bodyA = bodyB;
                nodeB = rootObj; bodyB = body;

                objectEditor.setSelectedObject(rootObj, body, (RobotManager.ShapeType) typeObj);

                if (!event.isControlDown()) {
                    multiSelection.clear();
                    multiSelection.add(rootObj);
                } else {
                    if (!multiSelection.contains(rootObj)) multiSelection.add(rootObj);
                }
            }
        }
        // 3. VACÍO (Caja de Selección)
        else {
            isDraggingObject = false;
            isBoxSelecting = true;

            // Coordenadas locales al contenedor del rectángulo
            Point2D localPoint = selectionRect.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
            startSelX = localPoint.getX();
            startSelY = localPoint.getY();

            setupRect(startSelX, startSelY, 0, 0, true);

            if (!event.isControlDown()) {
                objectEditor.setSelectedObject(null, null, null);
                nodeA = null; nodeB = null;
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
        isDraggingObject = false;
    }

    // --- LÓGICA DE MOVIMIENTO (SNAP) ---
    private void moveSelectedObject(MouseEvent event) {
        Node selectedNode = objectEditor.getCurrentNode();
        if (selectedNode == null) return;

        double mouseDx = event.getSceneX() - lastMouseX;
        double mouseDy = event.getSceneY() - lastMouseY;
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
        world3D.notifyObjectMovedManually(selectedNode);

        lastMouseX = event.getSceneX();
        lastMouseY = event.getSceneY();
    }

    // --- LÓGICA DE CAJA ---
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

        boolean foundAny = false;
        for (Node node : candidates) {
            javafx.geometry.Point3D posInScene3D = node.localToScene(0, 0, 0);
            javafx.geometry.Point3D posInScreen = world3D.getSubScene().localToScene(posInScene3D);

            if (rectBounds.contains(posInScreen.getX(), posInScreen.getY())) {
                if (!multiSelection.contains(node)) {
                    multiSelection.add(node);
                    foundAny = true;
                }
            }
        }

        if (foundAny) {
            Node last = multiSelection.get(multiSelection.size() - 1);
            RigidBody b = world3D.getPhysicsEngine().getBodyFromGraphic(last);
            Object t = last.getUserData();
            if (b != null && t instanceof RobotManager.ShapeType) {
                objectEditor.setSelectedObject(last, b, (RobotManager.ShapeType) t);
            }
        }
    }

    // --- UTILIDADES ---
    private boolean isGizmoPart(Node node) {
        while (node != null) {
            if (node instanceof TransformGizmo) return true;
            node = node.getParent();
        }
        return false;
    }

    private Node findRootObject(Node node) {
        while (node != null) {
            if (node.getUserData() instanceof RobotManager.ShapeType) return node;
            node = node.getParent();
        }
        return null;
    }

    // Getters para que el Controller acceda a la selección
    public List<Node> getMultiSelection() { return multiSelection; }
    public Node getNodeA() { return nodeA; }
    public Node getNodeB() { return nodeB; }
    public RigidBody getBodyA() { return bodyA; }
    public RigidBody getBodyB() { return bodyB; }
    public void clearSelection() {
        multiSelection.clear();
        nodeA = null; nodeB = null;
    }
}