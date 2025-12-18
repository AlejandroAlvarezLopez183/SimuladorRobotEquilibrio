package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;
import com.bulletphysics.dynamics.RigidBody;

import java.util.ArrayList;
import java.util.List;

public class RobotManager {

    public enum ShapeType { BOX, SPHERE, CYLINDER, RAMP }
    private final Group worldGroup;
    private final PhysicsEngine physics;
    private final World3D worldRef;

    private final List<RigidBody> bodies = new ArrayList<>();
    private final List<TransformGizmo> gizmos = new ArrayList<>();

    // 1. LISTA NUEVA PARA LAS CAJAS ROJAS
    private final List<Node> debugShapes = new ArrayList<>(); // <--- NUEVO

    private RigidBody activeBody;
    private TransformGizmo activeGizmo;

    private boolean areGizmosVisible = true;
    private boolean areDebugVisible = true; // <--- NUEVO (Estado por defecto)

    public RobotManager(Group worldGroup, PhysicsEngine physics, World3D worldRef) {
        this.worldGroup = worldGroup;
        this.physics = physics;
        this.worldRef = worldRef;
    }

    public RigidBody spawnRobot(Group nuevoModelo) {
        Bounds bounds = nuevoModelo.getBoundsInParent();
        float realWidth = Math.max(1.0f, (float) bounds.getWidth());
        float realHeight = Math.max(1.0f, (float) bounds.getHeight());
        float realDepth = Math.max(1.0f, (float) bounds.getDepth());

        Group robotActor = new Group();
        // ... (código de centrado igual que antes) ...
        double centerX = bounds.getMinX() + (bounds.getWidth() / 2);
        double centerY = bounds.getMinY() + (bounds.getHeight() / 2);
        double centerZ = bounds.getMinZ() + (bounds.getDepth() / 2);

        nuevoModelo.setTranslateX(-centerX);
        nuevoModelo.setTranslateY(-centerY);
        nuevoModelo.setTranslateZ(-centerZ);
        robotActor.getChildren().add(nuevoModelo);
        robotActor.setUserData(ShapeType.BOX);

        // AQUÍ SE CREA LA CAJA ROJA
        createDebugBox(robotActor, realWidth, realHeight, realDepth);

        // ... (código de posicionamiento igual que antes) ...
        double randomX = (Math.random() * 20) - 10;
        double randomZ = (Math.random() * 20) - 10;
        double alturaSpawn = -50 - (realHeight / 2);

        robotActor.setTranslateX(randomX);
        robotActor.setTranslateY(alturaSpawn);
        robotActor.setTranslateZ(randomZ);

        worldGroup.getChildren().add(robotActor);

        activeBody = physics.addBoxBody(robotActor, 10.0f, realWidth, realHeight, realDepth);
        bodies.add(activeBody);

        activeGizmo = new TransformGizmo(robotActor, worldRef);
        worldGroup.getChildren().add(activeGizmo);
        gizmos.add(activeGizmo);
        activeGizmo.setVisible(areGizmosVisible);

        return activeBody;
    }

    public RigidBody spawnPrimitive(Group model, ShapeType type, double sizeDim1, double sizeDim2) {
        // ... (código igual que antes) ...
        Group actor = new Group();
        actor.getChildren().add(model);

        double randomOffset = (Math.random() * 40) - 20;
        actor.setTranslateX(randomOffset);
        actor.setTranslateY(-50 - sizeDim1);
        actor.setTranslateZ(randomOffset);

        worldGroup.getChildren().add(actor);
        actor.setUserData(type);
        float mass = 5.0f;
        switch (type) {
            case SPHERE:
                activeBody = physics.addSphereBody(actor, mass, (float)sizeDim1);
                break;
            case CYLINDER:
                activeBody = physics.addCylinderBody(actor, mass, (float)sizeDim1, (float)sizeDim2);
                break;
            case RAMP:
                activeBody = physics.addRampBody(actor, mass, (float)sizeDim1);
                break;
            case BOX:
            default:
                activeBody = physics.addBoxBody(actor, mass, (float)sizeDim1, (float)sizeDim1, (float)sizeDim1);
                break;
        }
        bodies.add(activeBody);

        activeGizmo = new TransformGizmo(actor, worldRef);
        worldGroup.getChildren().add(activeGizmo);
        gizmos.add(activeGizmo);
        activeGizmo.setVisible(areGizmosVisible);

        return activeBody;
    }

    // 2. MODIFICAMOS ESTE MÉTODO PARA REGISTRAR LA CAJA
    private void createDebugBox(Group actor, float w, float h, float d) {
        Box debugBox = new Box(w, h, d);
        debugBox.setMaterial(new PhongMaterial(Color.RED));
        debugBox.setDrawMode(DrawMode.LINE);
        debugBox.setMouseTransparent(true);

        actor.getChildren().add(debugBox);

        // AÑADIR A LA LISTA Y APLICAR VISIBILIDAD
        debugShapes.add(debugBox);     // <--- Guardar referencia
        debugBox.setVisible(areDebugVisible); // <--- Aplicar estado actual
    }

    public void removeRobot(Node robotNode, RigidBody body) {
        bodies.remove(body);

        // Limpiar Gizmos
        gizmos.removeIf(g -> {
            if (g.getTargetNode() == robotNode) {
                worldGroup.getChildren().remove(g);
                return true;
            }
            return false;
        });

        // 3. LIMPIAR CAJAS ROJAS (Eliminamos de la lista si su padre es el nodo borrado)
        debugShapes.removeIf(node -> node.getParent() == robotNode);

        physics.removeBody(body);
        worldGroup.getChildren().remove(robotNode);
        System.out.println("Objeto eliminado correctamente.");
    }

    public void clearScene() {
        for (RigidBody b : bodies) physics.removeBody(b);
        bodies.clear();
        gizmos.clear();
        debugShapes.clear(); // <--- Limpiar lista

        activeBody = null;
        activeGizmo = null;

        while (worldGroup.getChildren().size() > 1) {
            worldGroup.getChildren().remove(1);
        }
        System.out.println(">> Escena Limpiada");
    }

    // ... (resetAllPositions, getLastGizmo, updateAllGizmos, setGizmosVisible igual que antes) ...
    public void resetAllPositions() {
        for (RigidBody b : bodies) resetOneBody(b);
        physics.updateGraphics();
    }

    private void resetOneBody(RigidBody b) { /* ... igual ... */
        if (b == null) return;
        com.bulletphysics.linearmath.Transform t = new com.bulletphysics.linearmath.Transform();
        t.setIdentity();
        b.getWorldTransform(t);
        t.origin.y = -80f;
        b.setWorldTransform(t);
        b.setLinearVelocity(new javax.vecmath.Vector3f(0,0,0));
        b.setAngularVelocity(new javax.vecmath.Vector3f(0,0,0));
        b.activate();
    }

    public TransformGizmo getLastGizmo() { return activeGizmo; }
    public void updateAllGizmos() { for (TransformGizmo g : gizmos) g.updatePosition(); }
    public void setGizmosVisible(boolean visible) {
        this.areGizmosVisible = visible;
        for (TransformGizmo g : gizmos) g.setVisible(visible);
    }

    // 4. NUEVO MÉTODO PARA MOSTRAR/OCULTAR CAJAS ROJAS
    public void setDebugVisible(boolean visible) {
        this.areDebugVisible = visible;
        for (Node box : debugShapes) {
            box.setVisible(visible);
        }
    }
}