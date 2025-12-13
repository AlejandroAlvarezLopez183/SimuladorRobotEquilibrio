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
    private final Group root3D = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;
    private PhysicsEngine physics;
    private AnimationTimer simulationLoop;

    // Grupo que contiene todo el mundo y rotará
    private final Group worldGroup = new Group();

    // Variables para el control del mouse (Cámara Orbital)
    private double mouseOldX, mouseOldY;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translateZ = new Translate(0, 0, -50); // Zoom inicial

    public World3D(double width, double height) {
        // 1. Configurar la escena 3D
        // true = depthBuffer (para que los objetos no se vean transparentes o raros)
        subScene = new SubScene(root3D, width, height, true, javafx.scene.SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1e1e1e")); // Fondo gris oscuro tipo Editor

        // 2. Configurar Cámara
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.getTransforms().addAll(rotateY, rotateX, translateZ);
        subScene.setCamera(camera);

        // 3. Crear contenido inicial
        buildWorld();
        setupMouseControl();

        root3D.getChildren().add(worldGroup);
        physics = new PhysicsEngine();

        // 4. Iniciar el Loop de Simulación (60 FPS)
        startSimulationLoop();
    }

    public PhysicsEngine getPhysicsEngine() {
        return physics;
    }

    private void buildWorld() {
        // --- A. El Suelo (Workplane) ---
        // Creamos una base de 1000x1000 unidades
        Group workplane = createWorkplane(1000);
        worldGroup.getChildren().add(workplane);

        // --- B. Iluminación ---
        javafx.scene.AmbientLight ambientLight = new javafx.scene.AmbientLight(Color.rgb(80, 80, 80));
        root3D.getChildren().add(ambientLight);

        javafx.scene.PointLight pointLight = new javafx.scene.PointLight(Color.WHITE);
        pointLight.setTranslateX(200);
        pointLight.setTranslateY(-300); // Luz desde arriba
        pointLight.setTranslateZ(-200);
        root3D.getChildren().add(pointLight);
    }

    // Lógica para rotar la cámara arrastrando el mouse
    private void setupMouseControl() {
        subScene.setOnMousePressed(event -> {
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        subScene.setOnMouseDragged(event -> {
            double dx = event.getSceneX() - mouseOldX;
            double dy = event.getSceneY() - mouseOldY;

            // Rotar mundo
            rotateY.setAngle(rotateY.getAngle() + dx * 0.3); // Velocidad giro horizontal
            rotateX.setAngle(rotateX.getAngle() - dy * 0.3); // Velocidad giro vertical

            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        // Zoom con la rueda del mouse
        subScene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY();
            double newZ = translateZ.getZ() + (delta * 0.5);
            // Límites del zoom
            if (newZ < -2 && newZ > -5000) {
                translateZ.setZ(newZ);
            }
        });
    }

    public com.bulletphysics.dynamics.RigidBody setRobotModel(Group nuevoModelo) {
        // 1. Limpiar (Igual que antes)
        while (worldGroup.getChildren().size() > 1) {
            worldGroup.getChildren().remove(1);
        }

        // --- 2. MEDICIÓN PRECISA (NUEVO) ---
        // Usamos getBoundsInParent() que suele incluir las transformaciones de los hijos (patas)
        Bounds bounds = nuevoModelo.getBoundsInParent();

        // Si sigue fallando, probaremos un factor de seguridad manual,
        // pero esto debería capturar las patas.
        float realWidth = (float) bounds.getWidth();
        float realHeight = (float) bounds.getHeight();
        float realDepth = (float) bounds.getDepth();

        // Validar tamaños mínimos (Safety Check)
        if (realWidth < 0.1f) realWidth = 1.0f;
        if (realHeight < 0.1f) realHeight = 1.0f;
        if (realDepth < 0.1f) realDepth = 1.0f;

        System.out.println("MEDIDAS -> W:" + realWidth + " H:" + realHeight + " D:" + realDepth);

        // 3. Crear el Wrapper (Actor)
        Group robotActor = new Group();

        // Centrar visualmente: IMPORTANTE
        // Usamos bounds.getMinX() + width/2 para encontrar el centro exacto de la geometría
        double centerX = bounds.getMinX() + (bounds.getWidth() / 2);
        double centerY = bounds.getMinY() + (bounds.getHeight() / 2);
        double centerZ = bounds.getMinZ() + (bounds.getDepth() / 2);

        nuevoModelo.setTranslateX(-centerX);
        nuevoModelo.setTranslateY(-centerY);
        nuevoModelo.setTranslateZ(-centerZ);

        robotActor.getChildren().add(nuevoModelo);

        // --- 4. DEBUG: DIBUJAR LA CAJA (HITBOX) ---
        Box debugBox = new Box(realWidth, realHeight, realDepth);
        debugBox.setMaterial(new PhongMaterial(Color.RED));
        debugBox.setDrawMode(DrawMode.LINE);
        debugBox.setMouseTransparent(true);
        robotActor.getChildren().add(debugBox);

        // 5. Posicionar el Actor (Spawn)
        // Spawn un poco más alto para asegurar caída
        double alturaSpawn = -50 - (realHeight / 2);
        robotActor.setTranslateY(alturaSpawn);

        worldGroup.getChildren().add(robotActor);

        // 6. Gizmo
        currentGizmo = new TransformGizmo(robotActor, this);
        worldGroup.getChildren().add(currentGizmo);

        // 7. Física
        return physics.addBoxBody(robotActor, 10.0f, realWidth, realHeight, realDepth);
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

    // Método puente para comunicar el Gizmo con la Física
    public void notifyObjectMovedManually(Node movedObject) {
        if (physics != null) {
            physics.updatePhysicsFromGraphicPosition(movedObject);
        }
    }

    public SubScene getSubScene() {
        return subScene;
    }
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
        // Aquí podrías llamar al reset del engine si lo necesitas
    }

    private Group createWorkplane(double size) {
        Group planeGroup = new Group();

        // 1. La base sólida (Azul claro estilo CAD)
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
    private Bounds getPreciseBounds(Node node) {
        Bounds bounds = node.getBoundsInLocal();
        if (node instanceof Group) {
            for (Node child : ((Group) node).getChildren()) {
                Bounds childBounds = getPreciseBounds(child);
                // Si el padre no tiene tamaño pero el hijo sí, usamos el del hijo
                if (bounds.isEmpty()) {
                    bounds = childBounds;
                } else {
                    // Si ambos tienen tamaño, los sumamos (Unión)
                    // Nota: Necesitas importar javafx.geometry.BoundingBox si da error,
                    // pero usualmente con Bounds basta si usamos la lógica correcta.
                    // Para simplificar, usaremos un truco de JavaFX:
                    // La unión de bounds se hace mejor acumulando min/max.
                }
            }
            return node.getBoundsInParent();
        }
        return bounds;
    }
}