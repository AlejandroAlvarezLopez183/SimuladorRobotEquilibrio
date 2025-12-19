package uv.simuladoraleexplorador.main.ui;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

// Imports de tu proyecto
import uv.simuladoraleexplorador.main.ui.view2d.InfiniteGrid2D;
import uv.simuladoraleexplorador.main.ui.view3d.ObjectLibraryUI;
import uv.simuladoraleexplorador.main.ui.view3d.World3D;
import uv.simuladoraleexplorador.main.ui.view3d.ObjectEditorUI;
import uv.simuladoraleexplorador.main.ui.view3d.SceneInteractionHandler; // <--- TU NUEVA CLASE
import uv.simuladoraleexplorador.main.utils.ObjLoader;
import uv.simuladoraleexplorador.main.model.physics.RigidBodyFactory; // Opcional si usas factory directo

// Imports JBullet / Matemáticas
import com.bulletphysics.dynamics.RigidBody;
import javax.vecmath.Quat4f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    // --- UI INJECTIONS (FXML) ---
    @FXML private StackPane contentPane;
    @FXML private javafx.scene.layout.VBox inspectorContainer;
    @FXML private Rectangle selectionRect; // El cuadro rojo punteado

    // Botones Superiores
    @FXML private ToggleButton btnGizmos;
    @FXML private ToggleButton btnDebug;
    @FXML private Button btn2D;
    @FXML private Button btn3D;
    @FXML private Button btnDelete;
    @FXML private ComboBox<Double> cmbSnap; // Selector de precisión (Tinkercad)

    // Inspector
    @FXML private TextField txtMasa;
    @FXML private Slider sliderDrag;
    @FXML private Label lblAltura;
    @FXML private Slider sliderRotX;

    // --- LÓGICA DEL SISTEMA ---
    private InfiniteGrid2D grid2D;
    private World3D world3D;
    private ObjectEditorUI objectEditor;

    // ¡LA NUEVA CLASE QUE MANEJA EL MOUSE!
    private SceneInteractionHandler interactionHandler;

    // Referencias temporales para importación
    private Group currentRobotModel;
    private RigidBody currentBody;

    @FXML
    public void initialize() {
        // 1. Inicializar Vistas
        initView2D();
        initView3D();

        // 2. Inicializar Paneles Laterales
        ObjectLibraryUI libraryUI = new ObjectLibraryUI(world3D.getRobotManager());
        inspectorContainer.getChildren().add(0, libraryUI.getView());

        objectEditor = new ObjectEditorUI(world3D.getPhysicsEngine());
        inspectorContainer.getChildren().add(1, objectEditor.getView());

        // 3. Configurar UI
        if (sliderRotX != null) sliderRotX.setValue(180);

        if (cmbSnap != null) {
            cmbSnap.getItems().addAll(0.1, 0.5, 1.0, 5.0, 10.0, 20.0);
            cmbSnap.setValue(1.0);
        }

        // Listener de Física (Drag/Masa)
        sliderDrag.valueProperty().addListener((obs, oldVal, newVal) -> handleUpdatePhysics());

        // Bindings
        btnDelete.disableProperty().bind(objectEditor.selectedNodeProperty().isNull());

        new ShortcutManager(this).init(contentPane);

        // Manejador del Mouse
        interactionHandler = new SceneInteractionHandler(world3D, objectEditor, selectionRect, cmbSnap);
    }

    public void iniciarSistema(Stage stage) {
        handleSwitchTo2D();
    }

    // --- GESTIÓN DE VISTAS ---

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

        // Re-agregar el overlay de selección encima de la subscene
        if (selectionRect != null && selectionRect.getParent() != null) {
            // Aseguramos que el Pane padre del rectángulo esté encima
            if (!contentPane.getChildren().contains(selectionRect.getParent())) {
                contentPane.getChildren().add(selectionRect.getParent());
            }
            selectionRect.getParent().toFront();
        }

        world3D.getSubScene().requestFocus();
        btn2D.setDisable(false);
        btn3D.setDisable(true);
    }

    // --- ACCIONES DE ARCHIVO / SIMULACIÓN ---

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

    // --- ACCIONES DE EDICIÓN (AGRUPAR / BORRAR) ---

    @FXML
    public void handleGroupObjects() {
        // Pedimos la lista de selección al Handler
        List<Node> selection = interactionHandler.getMultiSelection();

        if (selection.size() >= 2) {
            System.out.println("Solicitando agrupar " + selection.size() + " objetos...");

            // Enviamos la lista al RobotManager
            world3D.getRobotManager().groupObjects(new ArrayList<>(selection));

            // Limpiamos la selección visual
            interactionHandler.clearSelection();
            objectEditor.setSelectedObject(null, null, null);
        } else {
            System.out.println("⚠️ Selecciona al menos 2 objetos para agrupar (Usa selección de caja o CTRL+Clic).");
        }
    }

    @FXML
    public void handleUngroupObjects() {
        // Desagrupar el objeto seleccionado actualmente
        Node selected = objectEditor.getCurrentNode();
        RigidBody body = objectEditor.getCurrentBody();

        if (selected != null && body != null) {
            world3D.getRobotManager().ungroupObject(selected, body);
            objectEditor.setSelectedObject(null, null, null);
        }
    }

    @FXML
    public void handleDeleteSelected() {
        // 1. Borrar selección múltiple si existe
        List<Node> selection = interactionHandler.getMultiSelection();
        if (!selection.isEmpty()) {
            for (Node n : new ArrayList<>(selection)) {
                RigidBody b = world3D.getPhysicsEngine().getBodyFromGraphic(n);
                if (b != null) {
                    world3D.getRobotManager().removeRobot(n, b);
                }
            }
            interactionHandler.clearSelection();
        }
        // 2. Fallback: Borrar selección simple del editor
        else {
            Node n = objectEditor.getCurrentNode();
            RigidBody b = objectEditor.getCurrentBody();
            if (n != null && b != null) {
                world3D.getRobotManager().removeRobot(n, b);
            }
        }
        objectEditor.setSelectedObject(null, null, null);
    }

    // --- ACCIONES DE FÍSICA Y VISUALIZACIÓN ---

    @FXML public void handleRotarX() { aplicarRotacionFisica(90, 0, 0); }
    @FXML public void handleRotarY() { aplicarRotacionFisica(0, 90, 0); }
    @FXML public void handleRotarZ() { aplicarRotacionFisica(0, 0, 90); }

    private void aplicarRotacionFisica(float xDeg, float yDeg, float zDeg) {
        RigidBody target = objectEditor.getCurrentBody();
        if (target == null) target = currentBody; // Fallback al último importado

        if (target != null && world3D != null) {
            world3D.pause();
            com.bulletphysics.linearmath.Transform trans = new com.bulletphysics.linearmath.Transform();
            target.getWorldTransform(trans);

            Quat4f rotActual = new Quat4f();
            trans.getRotation(rotActual);

            Vector3f eje = new Vector3f(0,0,0);
            float angulo = 0;
            if (xDeg!=0) { eje.set(1,0,0); angulo=(float)Math.toRadians(xDeg); }
            else if (yDeg!=0) { eje.set(0,1,0); angulo=(float)Math.toRadians(yDeg); }
            else if (zDeg!=0) { eje.set(0,0,1); angulo=(float)Math.toRadians(zDeg); }

            AxisAngle4f aa = new AxisAngle4f(eje, angulo);
            Quat4f rotExtra = new Quat4f(); rotExtra.set(aa);
            rotActual.mul(rotExtra);
            trans.setRotation(rotActual);

            target.setWorldTransform(trans);
            target.setLinearVelocity(new Vector3f(0,0,0));
            target.setAngularVelocity(new Vector3f(0,0,0));
            target.activate();

            world3D.getPhysicsEngine().updateGraphics();
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

    @FXML
    public void handleToggleGizmos() {
        if (world3D != null) {
            world3D.getRobotManager().setGizmosVisible(btnGizmos.isSelected());
        }
    }

    @FXML
    public void handleToggleDebug() {
        if (world3D != null) {
            world3D.getRobotManager().setDebugVisible(btnDebug.isSelected());
        }
    }
}