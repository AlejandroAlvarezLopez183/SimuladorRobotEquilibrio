package uv.simuladoraleexplorador.main.ui.view3d;
import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.shape.DrawMode;
import javafx.scene.Group;
import javafx.scene.Node; // <--- ESTE IMPORT FALTABA
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;

public class World3D {

    private boolean isSimulating = false;
    private TransformGizmo currentGizmo;

    // Componentes Gráficos
    private final Group root3D = new Group();
    private final Group worldGroup = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;

    // Componentes Lógicos (Helpers)
    private PhysicsEngine physics;
    private AnimationTimer simulationLoop;
    private CameraController cameraController;
    private RobotManager robotManager;

    // Transformaciones de Cámara
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateZ = new Translate(0, 0, -50);

    public World3D(double width, double height) {
        subScene = new SubScene(root3D, width, height, true, javafx.scene.SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1e1e1e"));

        setupCamera();
        buildEnvironment();

        root3D.getChildren().add(worldGroup);
        physics = new PhysicsEngine();

        // 1. Control de Cámara (Esto reemplaza a setupMouseControl)
        cameraController = new CameraController(subScene, rotateX, rotateY, translateZ);

        // 2. Gestor de Robots (Esto reemplaza la lógica compleja de spawn)
        robotManager = new RobotManager(worldGroup, physics, this);

        startSimulationLoop();
    }

    public PhysicsEngine getPhysicsEngine() {
        return physics;
    }

    private void setupCamera() {
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.getTransforms().addAll(rotateY, rotateX, translateZ);
        subScene.setCamera(camera);
    }

    private void buildEnvironment() {
        // Creamos el suelo y las luces
        Group workplane = createWorkplane(1000);
        worldGroup.getChildren().add(workplane);

        javafx.scene.AmbientLight ambientLight = new javafx.scene.AmbientLight(Color.rgb(80, 80, 80));
        root3D.getChildren().add(ambientLight);

        javafx.scene.PointLight pointLight = new javafx.scene.PointLight(Color.WHITE);
        pointLight.setTranslateX(200);
        pointLight.setTranslateY(-300);
        pointLight.setTranslateZ(-200);
        root3D.getChildren().add(pointLight);
    }

    // Este es el método que daba error. Con el cambio en RobotManager, ahora funcionará.
    public com.bulletphysics.dynamics.RigidBody setRobotModel(Group nuevoModelo) {
        // Recibimos el Body desde el manager
        com.bulletphysics.dynamics.RigidBody body = robotManager.spawnRobot(nuevoModelo);
        // Actualizamos la referencia local del Gizmo
        this.currentGizmo = robotManager.getLastGizmo();

        return body;
    }

    private void startSimulationLoop() {
        simulationLoop = new AnimationTimer() {
            long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                float timeStep = (now - lastTime) / 1_000_000_000.0f;
                lastTime = now;

                if (isSimulating && physics != null) {
                    physics.stepSimulation(timeStep);
                }

                if (currentGizmo != null) {
                    currentGizmo.updatePosition();
                }
            }
        };
        simulationLoop.start();
    }

    public void notifyObjectMovedManually(Node movedObject) {
        if (physics != null) {
            physics.updatePhysicsFromGraphicPosition(movedObject);
        }
    }

    public SubScene getSubScene() { return subScene; }

    public void play() {
        isSimulating = true;
        System.out.println("Simulación: PLAY");
    }

    public void pause() {
        isSimulating = false;
        System.out.println("Simulación: PAUSA");
    }

    public void resetObjectPosition() {
        pause();
    }

    private Group createWorkplane(double size) {
        Group planeGroup = new Group();

        // 1. La base sólida
        Box base = new Box(size, 1.0, size);
        PhongMaterial baseMat = new PhongMaterial(Color.web("#e3f2fd"));
        baseMat.setSpecularColor(Color.WHITE);
        base.setMaterial(baseMat);
        base.setTranslateY(0.5);

        // 2. Líneas de Grid
        PhongMaterial gridMat = new PhongMaterial(Color.web("#90caf9"));
        double thickness = 0.5;

        for (double i = -size/2; i <= size/2; i += 50) {
            Box lineX = new Box(thickness, thickness, size);
            lineX.setMaterial(gridMat);
            lineX.setTranslateX(i);

            Box lineZ = new Box(size, thickness, thickness);
            lineZ.setMaterial(gridMat);
            lineZ.setTranslateZ(i);

            planeGroup.getChildren().addAll(lineX, lineZ);
        }
        planeGroup.getChildren().add(base);
        return planeGroup;
    }
}