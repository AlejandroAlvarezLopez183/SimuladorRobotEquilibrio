package uv.simuladoraleexplorador.main.ui.view3d;

import com.bulletphysics.dynamics.RigidBody;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;


public class RobotManager {

    private TransformGizmo lastGizmo;
    private final Group worldGroup;
    private final PhysicsEngine physics;
    private final World3D worldRef; // Necesario para el Gizmo
    private com.bulletphysics.dynamics.RigidBody lastBody;

    public RobotManager(Group worldGroup, PhysicsEngine physics, World3D worldRef) {
        this.worldGroup = worldGroup;
        this.physics = physics;
        this.worldRef = worldRef;
    }

    public com.bulletphysics.dynamics.RigidBody spawnRobot(Group nuevoModelo) {
        // 1. Limpiar anteriores
        cleanOldRobot();

        // 2. Calcular Bounds Precisos
        javafx.geometry.Bounds bounds = nuevoModelo.getBoundsInParent();
        float realWidth = Math.max(1.0f, (float) bounds.getWidth());
        float realHeight = Math.max(1.0f, (float) bounds.getHeight());
        float realDepth = Math.max(1.0f, (float) bounds.getDepth());

        // 3. Crear Wrapper (Actor)
        Group robotActor = new Group();

        double centerX = bounds.getMinX() + (bounds.getWidth() / 2);
        double centerY = bounds.getMinY() + (bounds.getHeight() / 2);
        double centerZ = bounds.getMinZ() + (bounds.getDepth() / 2);

        nuevoModelo.setTranslateX(-centerX);
        nuevoModelo.setTranslateY(-centerY);
        nuevoModelo.setTranslateZ(-centerZ);

        robotActor.getChildren().add(nuevoModelo);

        // 4. Debug Box
        createDebugBox(robotActor, realWidth, realHeight, realDepth);

        // 5. Posicionar
        double alturaSpawn = -50 - (realHeight / 2);
        robotActor.setTranslateY(alturaSpawn);
        worldGroup.getChildren().add(robotActor);

        // 6. Añadir a Física y guardar el BODY en una variable

        lastBody = physics.addBoxBody(robotActor, 10.0f, realWidth, realHeight, realDepth);

        // 7. Gizmo
        lastGizmo = new TransformGizmo(robotActor, worldRef);
        worldGroup.getChildren().add(lastGizmo);

        return lastBody;
    }

    private void cleanOldRobot() {
        // A. Limpiar Gráficos (Esto ya lo tenías)
        while (worldGroup.getChildren().size() > 1) {
            worldGroup.getChildren().remove(1);
        }

        // B. LIMPIEZA FÍSICA (ESTO ES LO NUEVO)
        if (lastBody != null) {
            physics.removeBody(lastBody);
            lastBody = null; // Reiniciamos la variable
            System.out.println(">> Robot Fantasma eliminado de la memoria física.");
        }
    }

    private void createDebugBox(Group actor, float w, float h, float d) {
        Box debugBox = new Box(w, h, d);
        debugBox.setMaterial(new PhongMaterial(Color.RED));
        debugBox.setDrawMode(DrawMode.LINE);
        debugBox.setMouseTransparent(true);
        actor.getChildren().add(debugBox);
    }
    public TransformGizmo getLastGizmo() {
        return lastGizmo;
    }
    public enum ShapeType {
        BOX, SPHERE, CYLINDER
    }

    public com.bulletphysics.dynamics.RigidBody spawnPrimitive(Group model, ShapeType type, double sizeDim1, double sizeDim2) {
        cleanOldRobot();

        // Wrapper (Actor)
        Group actor = new Group();
        actor.getChildren().add(model);

        // Posicionar alto
        actor.setTranslateY(-50 - sizeDim1);
        worldGroup.getChildren().add(actor);

        // Física según el tipo
        float mass = 5.0f;
        switch (type) {
            case SPHERE:
                lastBody = physics.addSphereBody(actor, mass, (float)sizeDim1);
                break;
            case CYLINDER:
                lastBody = physics.addCylinderBody(actor, mass, (float)sizeDim1, (float)sizeDim2);
                break;
            case BOX:
            default:
                // Para el cubo/rampa usaremos caja por simplicidad ahora
                lastBody = physics.addBoxBody(actor, mass, (float)sizeDim1, (float)sizeDim1, (float)sizeDim1);
                break;
        }

        // Gizmo
        lastGizmo = new TransformGizmo(actor, worldRef);
        worldGroup.getChildren().add(lastGizmo);

        return lastBody;
    }
}