package uv.simuladoraleexplorador.main.ui.view3d.robot;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;
import uv.simuladoraleexplorador.main.ui.view3d.RobotManager;
import uv.simuladoraleexplorador.main.ui.view3d.TransformGizmo;
import uv.simuladoraleexplorador.main.ui.view3d.World3D;
import com.bulletphysics.dynamics.RigidBody;

public class RobotSpawner {

    private final PhysicsEngine physics;
    private final Group worldGroup;
    private final World3D worldRef;
    private final RobotRegistry registry;

    public RobotSpawner(PhysicsEngine physics, Group worldGroup, World3D worldRef, RobotRegistry registry) {
        this.physics = physics;
        this.worldGroup = worldGroup;
        this.worldRef = worldRef;
        this.registry = registry;
    }

    public RigidBody spawnPrimitive(Group model, RobotManager.ShapeType type, double size1, double size2) {
        Group actor = new Group();
        actor.getChildren().add(model);

        // Posicionamiento aleatorio
        double offset = (Math.random() * 40) - 20;
        actor.setTranslateX(offset);
        actor.setTranslateY(-50 - size1);
        actor.setTranslateZ(offset);

        actor.setUserData(type);
        worldGroup.getChildren().add(actor);

        // Física
        RigidBody body = createPhysicsBody(actor, type, (float)size1, (float)size2);

        // Gizmo y Registro
        TransformGizmo gizmo = new TransformGizmo(actor, worldRef);
        worldGroup.getChildren().add(gizmo);

        registry.register(body, gizmo, null); // Primitivas no usan debug box roja por ahora

        return body;
    }

    public RigidBody spawnImported(Group model) {
        Bounds b = model.getBoundsInParent();
        float w = Math.max(1f, (float)b.getWidth());
        float h = Math.max(1f, (float)b.getHeight());
        float d = Math.max(1f, (float)b.getDepth());

        Group actor = new Group();
        // Centrado (Lógica simplificada)
        model.setTranslateX(-(b.getMinX() + w/2));
        model.setTranslateY(-(b.getMinY() + h/2));
        model.setTranslateZ(-(b.getMinZ() + d/2));

        actor.getChildren().add(model);
        actor.setUserData(RobotManager.ShapeType.BOX);

        // Debug Box (La caja roja)
        Box debugBox = createDebugBox(w, h, d);
        actor.getChildren().add(debugBox);

        // Posición
        actor.setTranslateY(-50 - h/2);
        worldGroup.getChildren().add(actor);

        // Física
        RigidBody body = physics.addBoxBody(actor, 10.0f, w, h, d);

        // Gizmo
        TransformGizmo gizmo = new TransformGizmo(actor, worldRef);
        worldGroup.getChildren().add(gizmo);

        registry.register(body, gizmo, debugBox);

        return body;
    }

    private RigidBody createPhysicsBody(Group node, RobotManager.ShapeType type, float s1, float s2) {
        float m = 5.0f;
        switch (type) {
            case SPHERE: return physics.addSphereBody(node, m, s1);
            case CYLINDER: return physics.addCylinderBody(node, m, s1, s2);
            case RAMP: return physics.addRampBody(node, m, s1);
            default: return physics.addBoxBody(node, m, s1, s1, s1);
        }
    }

    private Box createDebugBox(float w, float h, float d) {
        Box box = new Box(w, h, d);
        box.setMaterial(new PhongMaterial(Color.RED));
        box.setDrawMode(DrawMode.LINE);
        box.setMouseTransparent(true);
        return box;
    }
}