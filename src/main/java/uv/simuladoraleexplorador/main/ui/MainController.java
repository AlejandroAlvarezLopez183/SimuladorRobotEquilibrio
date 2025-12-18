package uv.simuladoraleexplorador.main.ui;

import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle; // Importante
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.geometry.Point2D; // Importante para matemáticas 2D

// Imports de Tu Proyecto
import uv.simuladoraleexplorador.main.ui.view2d.InfiniteGrid2D;
import uv.simuladoraleexplorador.main.ui.view3d.ObjectLibraryUI;
import uv.simuladoraleexplorador.main.ui.view3d.World3D;
import uv.simuladoraleexplorador.main.utils.ObjLoader;
import uv.simuladoraleexplorador.main.ui.view3d.ObjectEditorUI;

// Imports Matemáticos
import javax.vecmath.Quat4f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3f;
import com.bulletphysics.dynamics.RigidBody;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // --- UI PRINCIPAL ---
    private ObjectEditorUI objectEditor;
    @FXML private StackPane contentPane;
    @FXML private javafx.scene.layout.VBox inspectorContainer;

    // El rectángulo azul invisible (Asegúrate de tenerlo en el FXML)
    @FXML private Rectangle selectionRect;

    // --- BOTONES SUPERIORES ---
    @FXML private ToggleButton btnGizmos;
    @FXML private ToggleButton btnDebug;
    @FXML private Button btn2D;
    @FXML private Button btn3D;
    @FXML private Button btnDelete;
    @FXML private ComboBox<Double> cmbSnap;

    // --- CONTROLES INSPECTOR ---
    @FXML private TextField txtMasa;
    @FXML private Slider sliderDrag;
    @FXML private Label lblAltura;
    @FXML private Slider sliderRotX;

    // --- VARIABLES DE LÓGICA ---
    private InfiniteGrid2D grid2D;
    private World3D world3D;
    private double lastMouseX;
    private double lastMouseY;
    private boolean isDraggingObject = false;

    // --- VARIABLES PARA SELECCIÓN DE CAJA ---
    private boolean isBoxSelecting = false;
    private double startSelX, startSelY;
    private List<Node> multiSelection = new ArrayList<>(); // Lista de objetos seleccionados

    // Referencias al modelo actual
    private Group currentRobotModel;
    private RigidBody currentBody;

    // --- VARIABLES PARA AGRUPACIÓN (A + B) ---
    private Node nodeA, nodeB;
    private RigidBody bodyA, bodyB;

    @FXML
    public void initialize() {
        initView2D();
        initView3D();

        ObjectLibraryUI libraryUI = new ObjectLibraryUI(world3D.getRobotManager());
        inspectorContainer.getChildren().add(0, libraryUI.getView());

        objectEditor = new ObjectEditorUI(world3D.getPhysicsEngine());
        inspectorContainer.getChildren().add(1, objectEditor.getView());

        sliderDrag.valueProperty().addListener((obs, oldVal, newVal) -> handleUpdatePhysics());

        if (sliderRotX != null) sliderRotX.setValue(180);

        if (cmbSnap != null) {
            cmbSnap.getItems().addAll(0.1, 0.5, 1.0, 5.0, 10.0, 20.0);
            cmbSnap.setValue(1.0);
        }

        btnDelete.disableProperty().bind(objectEditor.selectedNodeProperty().isNull());

        setupSelectionHandler();
    }

    public void iniciarSistema(Stage stage) {
        handleSwitchTo2D();
    }

    // --- VISTAS ---
    private void initView2D() {
        grid2D = new InfiniteGrid2D(1000, 1000);
        grid2D.widthProperty().bind(contentPane.widthProperty());
        grid2D.heightProperty().bind(contentPane.heightProperty());
    }

    private void initView3D() {
        world3D = new World3D(800, 600);
        world3D.getSubScene().widthProperty().bind(contentPane.widthProperty());
        world3D.getSubScene().heightProperty().bind(contentPane.heightProperty());
    }

    @FXML
    public void handleSwitchTo2D() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(grid2D);
        grid2D.draw();
        btn2D.setDisable(true);
        btn3D.setDisable(false);
    }

    @FXML
    public void handleSwitchTo3D() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(world3D.getSubScene());
        if (selectionRect != null) {
            // Re-agregar el rectángulo encima de la subscene si se borró
            if (!contentPane.getChildren().contains(selectionRect)) {
                contentPane.getChildren().add(selectionRect);
            }
            selectionRect.toFront(); // Que siempre esté encima
        }
        world3D.getSubScene().requestFocus();
        btn2D.setDisable(false);
        btn3D.setDisable(true);
    }

    // --- IMPORTAR / SIMULAR ---
    @FXML
    public void handleImportarModelo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Modelo Robot (.obj)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Modelos 3D OBJ", "*.obj"));
        File selectedFile = fileChooser.showOpenDialog(contentPane.getScene().getWindow());

        if (selectedFile != null) {
            handleSimPause();
            handleSwitchTo3D();
            currentRobotModel = ObjLoader.loadModel(selectedFile);
            currentRobotModel.getTransforms().add(new Rotate(90, Rotate.X_AXIS));

            if (world3D != null) {
                this.currentBody = world3D.setRobotModel(currentRobotModel);
                txtMasa.setText("10.0");
                if (sliderDrag != null) sliderDrag.setValue(0.0);
                world3D.pause();
            }
        }
    }

    @FXML
    public void handleResetSim() {
        if (world3D != null) {
            world3D.pause();
            world3D.getRobotManager().resetAllPositions();
        }
    }

    @FXML public void handleSimPlay() { if (world3D != null) world3D.play(); }
    @FXML public void handleSimPause() { if (world3D != null) world3D.pause(); }

    // --- AGRUPAR ---
    @FXML
    public void handleGroupObjects() {
        if (nodeA != null && nodeB != null && nodeA != nodeB) {
            System.out.println("Uniendo " + nodeB + " dentro de " + nodeA);
            world3D.getRobotManager().mergeObjectB_into_ObjectA(nodeA, bodyA, nodeB, bodyB);
            nodeA = null; nodeB = null;
            objectEditor.setSelectedObject(null, null, null);
        } else {
            System.out.println("⚠️ Selecciona Padre luego Hijo para unir.");
        }
    }

    // --- FÍSICA Y ROTACIÓN ---
    @FXML public void handleRotarX() { aplicarRotacionFisica(90, 0, 0); }
    @FXML public void handleRotarY() { aplicarRotacionFisica(0, 90, 0); }
    @FXML public void handleRotarZ() { aplicarRotacionFisica(0, 0, 90); }

    private void aplicarRotacionFisica(float xDeg, float yDeg, float zDeg) {
        if (currentBody != null && world3D != null) {
            world3D.pause();
            com.bulletphysics.linearmath.Transform trans = new com.bulletphysics.linearmath.Transform();
            currentBody.getWorldTransform(trans);
            Quat4f rotActual = new Quat4f();
            trans.getRotation(rotActual);

            Vector3f eje = new Vector3f(0,0,0);
            float angulo = 0;
            if (xDeg!=0) { eje.set(1,0,0); angulo=(float)Math.toRadians(xDeg); }
            else if (yDeg!=0) { eje.set(0,1,0); angulo=(float)Math.toRadians(yDeg); }
            else if (zDeg!=0) { eje.set(0,0,1); angulo=(float)Math.toRadians(zDeg); }
            else return;

            AxisAngle4f aa = new AxisAngle4f(eje, angulo);
            Quat4f rotExtra = new Quat4f(); rotExtra.set(aa);
            rotActual.mul(rotExtra);
            trans.setRotation(rotActual);

            currentBody.setWorldTransform(trans);
            currentBody.setLinearVelocity(new Vector3f(0,0,0));
            currentBody.setAngularVelocity(new Vector3f(0,0,0));
            currentBody.activate();

            if (currentRobotModel!=null) world3D.getPhysicsEngine().updateGraphics();
        }
    }

    @FXML
    public void handleUpdatePhysics() {
        RigidBody target = objectEditor.getCurrentBody();
        if (target == null) target = currentBody;
        if (target != null && world3D != null) {
            try {
                float m = Float.parseFloat(txtMasa.getText());
                float d = (float) sliderDrag.getValue();
                world3D.getPhysicsEngine().updateBodyProperties(target, m, d);
            } catch(Exception e) {}
        }
    }

    // =========================================================
    //    CONTROL DE MOUSE: SELECCIÓN + ARRASTRE + CAJA
    // =========================================================
    private void setupSelectionHandler() {
        // Escuchamos eventos en la SubScene (donde ocurre la acción 3D)
        world3D.getSubScene().addEventHandler(javafx.scene.input.MouseEvent.ANY, event -> {

            // --- 1. CLIC (PRESSED) ---
            if (event.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
                if (event.isPrimaryButtonDown()) {
                    lastMouseX = event.getSceneX();
                    lastMouseY = event.getSceneY();

                    Node pickedNode = event.getPickResult().getIntersectedNode();
                    Node rootObj = findRootObject(pickedNode);

                    // A. GIZMO -> No hacer nada (dejar que el gizmo actúe)
                    if (isGizmoPart(pickedNode)) {
                        isDraggingObject = false;
                        isBoxSelecting = false;
                        return;
                    }

                    // B. OBJETO -> Selección Simple / Preparar Arrastre
                    if (rootObj != null) {
                        isDraggingObject = true;
                        isBoxSelecting = false;

                        RigidBody body = world3D.getPhysicsEngine().getBodyFromGraphic(rootObj);
                        Object typeObj = rootObj.getUserData();

                        if (body != null && typeObj instanceof uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType) {
                            // Lógica de Agrupación (A -> B)
                            nodeA = nodeB;
                            bodyA = bodyB;
                            nodeB = rootObj;
                            bodyB = body;

                            // Actualizar Inspector
                            objectEditor.setSelectedObject(rootObj, body, (uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType) typeObj);

                            // Gestión de Multiselección (CTRL)
                            if (!event.isControlDown()) {
                                multiSelection.clear();
                                multiSelection.add(rootObj);
                            } else {
                                if (!multiSelection.contains(rootObj)) multiSelection.add(rootObj);
                            }
                        }
                    }
                    // C. VACÍO -> Iniciar CAJA DE SELECCIÓN
                    else {
                        isDraggingObject = false;
                        isBoxSelecting = true;

                        // CORRECCIÓN VITAL: Convertir coordenada de pantalla al espacio del Panel del rectángulo
                        // Esto asegura que el rectángulo empiece EXACTAMENTE bajo el mouse
                        Point2D localPoint = selectionRect.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());

                        startSelX = localPoint.getX();
                        startSelY = localPoint.getY();

                        // Configurar rectángulo visual inicial
                        selectionRect.setX(startSelX);
                        selectionRect.setY(startSelY);
                        selectionRect.setWidth(0);
                        selectionRect.setHeight(0);
                        selectionRect.setVisible(true);

                        // Limpiar selección previa (si no hay CTRL)
                        if (!event.isControlDown()) {
                            objectEditor.setSelectedObject(null, null, null);
                            nodeA = null; nodeB = null;
                            multiSelection.clear();
                        }
                    }
                }
            }

            // --- 2. ARRASTRE (DRAGGED) ---
            else if (event.getEventType() == javafx.scene.input.MouseEvent.MOUSE_DRAGGED) {
                if (event.isPrimaryButtonDown()) {

                    // CASO A: MOVER OBJETO
                    if (isDraggingObject) {
                        Node selectedNode = objectEditor.getCurrentNode();
                        if (selectedNode != null) {
                            double mouseDx = event.getSceneX() - lastMouseX;
                            double mouseDy = event.getSceneY() - lastMouseY;

                            double camAngle = Math.toRadians(world3D.getCameraAngleY());
                            double sens = 0.5;

                            double moveX = (mouseDx * Math.cos(camAngle)) - (mouseDy * Math.sin(camAngle));
                            double moveZ = (mouseDx * Math.sin(camAngle)) + (mouseDy * Math.cos(camAngle));

                            double rawX = selectedNode.getTranslateX() + (moveX * sens);
                            double rawZ = selectedNode.getTranslateZ() + (moveZ * sens);

                            // SNAP TO GRID
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
                    }

                    // CASO B: DIBUJAR CAJA AZUL
                    else if (isBoxSelecting) {
                        // Convertir mouse actual al espacio del rectángulo
                        Point2D currentLocal = selectionRect.getParent().sceneToLocal(event.getSceneX(), event.getSceneY());

                        double currentX = currentLocal.getX();
                        double currentY = currentLocal.getY();

                        // Calcular matemáticas para permitir arrastre inverso (izquierda/arriba)
                        double x = Math.min(startSelX, currentX);
                        double y = Math.min(startSelY, currentY);
                        double w = Math.abs(currentX - startSelX);
                        double h = Math.abs(currentY - startSelY);

                        selectionRect.setX(x);
                        selectionRect.setY(y);
                        selectionRect.setWidth(w);
                        selectionRect.setHeight(h);
                    }
                }
            }

            // --- 3. SOLTAR (RELEASED) ---
            else if (event.getEventType() == javafx.scene.input.MouseEvent.MOUSE_RELEASED) {
                if (isBoxSelecting) {
                    performBoxSelection(); // <--- Llamada a la detección
                    selectionRect.setVisible(false);
                    isBoxSelecting = false;
                }
                isDraggingObject = false;
            }
        });
    }

    // --- LÓGICA MATEMÁTICA DE SELECCIÓN DE CAJA ---
    private void performBoxSelection() {
        List<Node> candidates = world3D.getRobotManager().getTrackedNodes();

        // 1. Obtener los límites REALES del rectángulo azul en la pantalla
        Bounds rectBoundsInScene = selectionRect.localToScene(selectionRect.getBoundsInLocal());

        boolean foundAny = false;
        multiSelection.clear(); // Limpiamos selección anterior si es nueva caja

        for (Node node : candidates) {
            // 2. Proyectar el centro del objeto 3D a la pantalla 2D (Scene Coordinates)
            // node.localToScene(0,0,0) -> Posición 3D en SubScene
            // subScene.localToScene(...) -> Posición 2D en la Ventana Principal
            Point3D posInScene = world3D.getSubScene().localToScene(node.localToScene(0, 0, 0));

            // 3. Verificar si el rectángulo de pantalla contiene ese punto de pantalla
            if (rectBoundsInScene.contains(posInScene.getX(), posInScene.getY())) {
                if (!multiSelection.contains(node)) {
                    multiSelection.add(node);
                    foundAny = true;

                    // Efecto visual opcional: Imprimir nombre para debug
                    System.out.println("Detectado: " + node.getUserData());
                }
            }
        }

        System.out.println("Objetos en caja: " + multiSelection.size());

        // Seleccionar el último encontrado en el editor
        if (foundAny) {
            Node last = multiSelection.get(multiSelection.size() - 1);
            RigidBody b = world3D.getPhysicsEngine().getBodyFromGraphic(last);
            // ... tu lógica de set selection ...
            // Importante: No olvides actualizar el objectEditor aquí
            Object typeObj = last.getUserData();
            if (b != null && typeObj instanceof uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType) {
                objectEditor.setSelectedObject(last, b, (uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType)typeObj);
            }
        }
    }
    // --- UTILIDADES ---
    private boolean isGizmoPart(javafx.scene.Node node) {
        while (node != null) {
            if (node instanceof uv.simuladoraleexplorador.main.ui.view3d.TransformGizmo) return true;
            node = node.getParent();
        }
        return false;
    }

    private javafx.scene.Node findRootObject(javafx.scene.Node node) {
        while (node != null) {
            if (node.getUserData() instanceof uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType) return node;
            node = node.getParent();
        }
        return null;
    }

    @FXML
    public void handleDeleteSelected() {
        // Si hay multiselección, borrar todo
        if (!multiSelection.isEmpty()) {
            for (Node n : new ArrayList<>(multiSelection)) {
                RigidBody b = world3D.getPhysicsEngine().getBodyFromGraphic(n);
                if (b != null) {
                    if (n == nodeA) { nodeA = null; bodyA = null; }
                    if (n == nodeB) { nodeB = null; bodyB = null; }
                    world3D.getRobotManager().removeRobot(n, b);
                }
            }
            multiSelection.clear();
            objectEditor.setSelectedObject(null, null, null);
        } else {
            // Fallback: borrar solo el actual del inspector
            Node n = objectEditor.getCurrentNode();
            RigidBody b = objectEditor.getCurrentBody();
            if (n!=null && b!=null) {
                world3D.getRobotManager().removeRobot(n, b);
                objectEditor.setSelectedObject(null, null, null);
            }
        }
    }

    @FXML public void handleToggleGizmos() { if (world3D!=null) world3D.getRobotManager().setGizmosVisible(btnGizmos.isSelected()); }
    @FXML public void handleToggleDebug() { if (world3D!=null) world3D.getRobotManager().setDebugVisible(btnDebug.isSelected()); }
}