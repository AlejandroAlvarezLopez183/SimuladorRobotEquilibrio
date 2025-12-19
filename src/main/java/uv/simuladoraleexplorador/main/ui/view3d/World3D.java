package uv.simuladoraleexplorador.main.ui.view3d;
import com.bulletphysics.dynamics.RigidBody;
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

import java.util.Map;

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
        EnvironmentBuilder.build(root3D, worldGroup);

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
            // Variable temporal para no crear basura en cada frame
            com.bulletphysics.linearmath.Transform tempT = new com.bulletphysics.linearmath.Transform();

            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                float timeStep = (now - lastTime) / 1_000_000_000.0f;
                lastTime = now;

                if (isSimulating && physics != null) {
                    // Paso 1: Simulación Física
                    physics.stepSimulation(timeStep);

                    // Paso 2: Kill Floor (Optimización)
                    // Iteramos sobre los cuerpos para ver si alguno cayó al vacío
                    for (com.bulletphysics.dynamics.RigidBody body : physics.getPhysicsToGraphicsMap().keySet()) {

                        // Obtenemos la posición física real
                        body.getMotionState().getWorldTransform(tempT);

                        // Si la altura (Y) es menor a -200 (cayó muy abajo)
                        if (tempT.origin.y < -200) {
                            // Opción A: Congelarlo para ahorrar CPU
                            body.setLinearVelocity(new javax.vecmath.Vector3f(0,0,0));
                            body.setAngularVelocity(new javax.vecmath.Vector3f(0,0,0));
                            body.forceActivationState(com.bulletphysics.dynamics.RigidBody.WANTS_DEACTIVATION);

                            // Opción B (Opcional): Si prefieres resetearlo al cielo descomenta esto:
                            /*
                            tempT.setIdentity();
                            tempT.origin.set(0, 50, 0); // Al cielo
                            body.setWorldTransform(tempT);
                            body.activate();
                            */
                        }
                    }
                }

                // El Gizmo debe actualizarse siempre, incluso en pausa
                if (robotManager != null) {
                    robotManager.updateAllGizmos();
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

    public RobotManager getRobotManager() {
        return robotManager;
    }
    public double getCameraAngleY() {
        return rotateY.getAngle();
    }

}