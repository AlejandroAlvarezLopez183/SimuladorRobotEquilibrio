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
        com.bulletphysics.dynamics.RigidBody body = physics.addBoxBody(robotActor, 10.0f, realWidth, realHeight, realDepth);

        // 7. Crear Gizmo y guardarlo
        lastGizmo = new TransformGizmo(robotActor, worldRef);
        worldGroup.getChildren().add(lastGizmo);

        // 8. RETORNAR EL BODY (Esto es lo que World3D está esperando)
        return body;
    }

    private void cleanOldRobot() {
        // Mantenemos index 0 (suelo)
        while (worldGroup.getChildren().size() > 1) {
            worldGroup.getChildren().remove(1);
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
}