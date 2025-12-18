package uv.simuladoraleexplorador.main.ui;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Group;
// OJO: Solo importamos Rotate de JavaFX, NO Transform
import javafx.scene.transform.Rotate;

// Imports de Tu Proyecto
import uv.simuladoraleexplorador.main.ui.view2d.InfiniteGrid2D;
import uv.simuladoraleexplorador.main.ui.view3d.ObjectLibraryUI;
import uv.simuladoraleexplorador.main.ui.view3d.World3D;
import uv.simuladoraleexplorador.main.utils.ObjLoader;
import uv.simuladoraleexplorador.main.ui.view3d.ObjectEditorUI;

// Imports Matemáticos (JBullet y Vecmath)
import javax.vecmath.Quat4f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3f;
import java.io.File;

public class MainController {
    private ObjectEditorUI objectEditor;
    @FXML private StackPane contentPane;
    @FXML private Button btn2D;
    @FXML private Button btn3D;
    @FXML private Button btnDelete;
    @FXML private javafx.scene.layout.VBox inspectorContainer;
    // Controles del Inspector
    @FXML private TextField txtMasa;
    @FXML private Slider sliderDrag;
    @FXML private Label lblAltura;
    // (Ya no necesitamos el sliderRotX en el initialize, pero si está en el FXML no estorba)
    @FXML private Slider sliderRotX;

    private InfiniteGrid2D grid2D;
    private World3D world3D;
    private double lastMouseX;
    private double lastMouseY;
    private boolean isDraggingObject = false;

    // Referencias al modelo actual
    private Group currentRobotModel;
    private com.bulletphysics.dynamics.RigidBody currentBody;

    @FXML
    public void initialize() {
        initView2D();
        initView3D();

        ObjectLibraryUI libraryUI = new ObjectLibraryUI(world3D.getRobotManager());
        inspectorContainer.getChildren().add(0, libraryUI.getView());

        objectEditor = new ObjectEditorUI(world3D.getPhysicsEngine());
        inspectorContainer.getChildren().add(1, objectEditor.getView());
        // Listener para actualizar la resistencia del aire en tiempo real
        sliderDrag.valueProperty().addListener((obs, oldVal, newVal) -> {
            handleUpdatePhysics();
        });

        // --- CORRECCIÓN ROTACIÓN ---
        // Sincronizar el slider con el valor 180 por defecto si existe
        if (sliderRotX != null) {
            sliderRotX.setValue(180);
        }
        btnDelete.disableProperty().bind(objectEditor.selectedNodeProperty().isNull());
        setupSelectionHandler();
    }

    public void iniciarSistema(Stage stage) {
        handleSwitchTo2D();
    }
    @FXML
    public void handleRotarX() {
        aplicarRotacionFisica(90, 0, 0); // Rotar 90 en X
    }

    @FXML
    public void handleRotarY() {
        aplicarRotacionFisica(0, 90, 0); // Rotar 90 en Y
    }

    @FXML
    public void handleRotarZ() {
        aplicarRotacionFisica(0, 0, 90); // Rotar 90 en Z
    }
    // --- Lógica de Vistas ---
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
        world3D.getSubScene().requestFocus();
        btn2D.setDisable(false);
        btn3D.setDisable(true);
    }

    // --- IMPORTACIÓN Y SIMULACIÓN ---

    @FXML
    public void handleImportarModelo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Modelo Robot (.obj)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Modelos 3D OBJ", "*.obj"));

        File selectedFile = fileChooser.showOpenDialog(contentPane.getScene().getWindow());

        if (selectedFile != null) {
            // 1. Pausar antes de cargar nada
            handleSimPause();
            handleSwitchTo3D();

            // 2. Cargar modelo
            currentRobotModel = ObjLoader.loadModel(selectedFile);

            currentRobotModel.getTransforms().add(new Rotate(90, Rotate.X_AXIS));

            // 3. Crear física
            if (world3D != null) {
                this.currentBody = world3D.setRobotModel(currentRobotModel);

                // Valores por defecto
                txtMasa.setText("10.0");
                if (sliderDrag != null) sliderDrag.setValue(0.0);

                // Aseguramos pausa
                world3D.pause();
            }
        }
    }

    @FXML
    public void handleResetSim() {
        if (world3D != null) {
            world3D.pause();
            // Le decimos al manager: "¡Sube a todos al cielo!"
            world3D.getRobotManager().resetAllPositions();
        }
    }

    @FXML
    public void handleSimPlay() {
        if (world3D != null) world3D.play();
    }

    @FXML
    public void handleSimPause() {
        if (world3D != null) world3D.pause();
    }

    private void aplicarRotacionFisica(float xDeg, float yDeg, float zDeg) {
        if (currentBody != null && world3D != null) {
            // 1. Pausar simulación
            world3D.pause();

            // 2. Obtener transformación (USANDO EL NOMBRE COMPLETO PARA EVITAR ERROR)
            com.bulletphysics.linearmath.Transform trans = new com.bulletphysics.linearmath.Transform();
            currentBody.getWorldTransform(trans);

            // 3. Obtener rotación actual
            Quat4f rotacionActual = new Quat4f();
            trans.getRotation(rotacionActual);

            // 4. Calcular la nueva rotación (extra)
            Quat4f rotacionExtra = new Quat4f();

            // Definir el eje de rotación
            Vector3f eje = new Vector3f(0,0,0);
            float anguloRad = 0;

            if (xDeg != 0) {
                eje.set(1, 0, 0); // Eje X
                anguloRad = (float) Math.toRadians(xDeg);
            } else if (yDeg != 0) {
                eje.set(0, 1, 0); // Eje Y
                anguloRad = (float) Math.toRadians(yDeg);
            } else if (zDeg != 0) {
                eje.set(0, 0, 1); // Eje Z
                anguloRad = (float) Math.toRadians(zDeg);
            } else {
                return; // Si todo es 0, no hacemos nada
            }

            // Crear el cuaternión usando AxisAngle4f
            AxisAngle4f axisAngle = new AxisAngle4f(eje, anguloRad);
            rotacionExtra.set(axisAngle);

            // 5. Multiplicar (Combinar rotaciones)
            rotacionActual.mul(rotacionExtra);

            // 6. Aplicar cambios
            trans.setRotation(rotacionActual);
            currentBody.setWorldTransform(trans);

            // 7. Limpiar velocidades para que no salga disparado
            currentBody.setLinearVelocity(new Vector3f(0,0,0));
            currentBody.setAngularVelocity(new Vector3f(0,0,0));
            currentBody.activate();

            // 8. Actualizar visualmente
            if (currentRobotModel != null) {
                // Pequeño paso para sincronizar gráfico
                world3D.getPhysicsEngine().updateGraphics();
            }

            System.out.println("Rotación de " + (xDeg+yDeg+zDeg) + "° aplicada correctamente.");
        }
    }
    @FXML
    public void handleUpdatePhysics() {
        if (currentBody != null && world3D != null) {
            try {
                float masa = Float.parseFloat(txtMasa.getText());
                float drag = (float) sliderDrag.getValue();
                world3D.getPhysicsEngine().updateBodyProperties(currentBody, masa, drag);
                System.out.println("Físicas actualizadas -> Masa: " + masa + ", Drag: " + drag);
            } catch (Exception e) {
                System.err.println("Error en valores de física");
            }
        }
    }

    private void setupSelectionHandler() {
        world3D.getSubScene().addEventHandler(javafx.scene.input.MouseEvent.ANY, event -> {

            // --- 1. CLIC (Detectar selección) ---
            if (event.getEventType() == javafx.scene.input.MouseEvent.MOUSE_PRESSED) {
                if (event.isPrimaryButtonDown()) {
                    lastMouseX = event.getSceneX();
                    lastMouseY = event.getSceneY();

                    javafx.scene.Node pickedNode = event.getPickResult().getIntersectedNode();
                    javafx.scene.Node rootObj = findRootObject(pickedNode);

                    // IMPORTANTE: Si tocamos el GIZMO, dejamos que el Gizmo haga su trabajo (no arrastramos el cuerpo)
                    if (isGizmoPart(pickedNode)) {
                        isDraggingObject = false;
                        return;
                    }

                    if (rootObj != null) {
                        com.bulletphysics.dynamics.RigidBody body = world3D.getPhysicsEngine().getBodyFromGraphic(rootObj);
                        Object typeObj = rootObj.getUserData();

                        if (body != null && typeObj instanceof uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType) {
                            objectEditor.setSelectedObject(rootObj, body, (uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType) typeObj);
                            isDraggingObject = true;
                        }
                    } else {
                        objectEditor.setSelectedObject(null, null, null);
                        isDraggingObject = false;
                    }
                }
            }

            // --- 2. ARRASTRE TIPO TINKERCAD (Suelo X/Z relativo a la cámara) ---
            else if (event.getEventType() == javafx.scene.input.MouseEvent.MOUSE_DRAGGED) {
                if (event.isPrimaryButtonDown() && isDraggingObject) {
                    javafx.scene.Node selectedNode = objectEditor.getCurrentNode();

                    if (selectedNode != null) {
                        double mouseDx = event.getSceneX() - lastMouseX;
                        double mouseDy = event.getSceneY() - lastMouseY;

                        // --- MAGIA MATEMÁTICA ---
                        // 1. Obtener el ángulo Y de la cámara (hacia dónde miramos)
                        // Nota: Necesitamos acceder a la cámara. Asumo que está en world3D.
                        double cameraAngleRad = Math.toRadians(world3D.getCameraAngleY());

                        double sensitivity = 0.5; // Ajustar velocidad

                        // 2. Rotar el vector del mouse según la cámara
                        // Fórmula de rotación 2D para mapear pantalla a mundo 3D
                        double moveX = (mouseDx * Math.cos(cameraAngleRad)) - (mouseDy * Math.sin(cameraAngleRad));
                        double moveZ = (mouseDx * Math.sin(cameraAngleRad)) + (mouseDy * Math.cos(cameraAngleRad));

                        // 3. Aplicar movimiento (SOLO EN X y Z -> Suelo)
                        selectedNode.setTranslateX(selectedNode.getTranslateX() + (moveX * sensitivity));
                        selectedNode.setTranslateZ(selectedNode.getTranslateZ() + (moveZ * sensitivity));

                        // 4. Actualizar Física
                        world3D.notifyObjectMovedManually(selectedNode);

                        lastMouseX = event.getSceneX();
                        lastMouseY = event.getSceneY();
                    }
                }
            }

            // --- 3. SOLTAR ---
            else if (event.getEventType() == javafx.scene.input.MouseEvent.MOUSE_RELEASED) {
                isDraggingObject = false;
            }
        });
    }

    // Método auxiliar para detectar si clicamos una flecha del Gizmo
    private boolean isGizmoPart(javafx.scene.Node node) {
        while (node != null) {
            if (node instanceof uv.simuladoraleexplorador.main.ui.view3d.TransformGizmo) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }
    private javafx.scene.Node findRootObject(javafx.scene.Node node) {
        while (node != null) {
            if (node.getUserData() instanceof uv.simuladoraleexplorador.main.ui.view3d.RobotManager.ShapeType) {
                return node;
            }
            node = node.getParent();
        }
        return null; // No es un objeto editable
    }
    @FXML
    public void handleDeleteSelected() {
        Node selectedNode = objectEditor.getCurrentNode();
        com.bulletphysics.dynamics.RigidBody selectedBody = objectEditor.getCurrentBody();

        if (selectedNode != null && selectedBody != null) {
            // Ejecutar eliminación
            world3D.getRobotManager().removeRobot(selectedNode, selectedBody);

            // Limpiar el editor para que no muestre controles de algo que ya no existe
            objectEditor.setSelectedObject(null, null, null);
        } else {
            System.out.println("No hay ningún objeto seleccionado para eliminar.");
        }
    }

}