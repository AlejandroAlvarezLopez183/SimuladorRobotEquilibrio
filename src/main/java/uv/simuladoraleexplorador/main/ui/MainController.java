package uv.simuladoraleexplorador.main.ui;

import javafx.fxml.FXML;
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

// Imports Matemáticos (JBullet y Vecmath)
import javax.vecmath.Quat4f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3f;
import java.io.File;

public class MainController {

    @FXML private StackPane contentPane;
    @FXML private Button btn2D;
    @FXML private Button btn3D;
    @FXML private javafx.scene.layout.VBox inspectorContainer;
    // Controles del Inspector
    @FXML private TextField txtMasa;
    @FXML private Slider sliderDrag;
    @FXML private Label lblAltura;
    // (Ya no necesitamos el sliderRotX en el initialize, pero si está en el FXML no estorba)
    @FXML private Slider sliderRotX;

    private InfiniteGrid2D grid2D;
    private World3D world3D;

    // Referencias al modelo actual
    private Group currentRobotModel;
    private com.bulletphysics.dynamics.RigidBody currentBody;

    @FXML
    public void initialize() {
        initView2D();
        initView3D();

        ObjectLibraryUI libraryUI = new ObjectLibraryUI(world3D.getRobotManager());
        inspectorContainer.getChildren().add(0, libraryUI.getView());
        // Listener para actualizar la resistencia del aire en tiempo real
        sliderDrag.valueProperty().addListener((obs, oldVal, newVal) -> {
            handleUpdatePhysics();
        });

        // --- CORRECCIÓN ROTACIÓN ---
        // Sincronizar el slider con el valor 180 por defecto si existe
        if (sliderRotX != null) {
            sliderRotX.setValue(180);
        }
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
}