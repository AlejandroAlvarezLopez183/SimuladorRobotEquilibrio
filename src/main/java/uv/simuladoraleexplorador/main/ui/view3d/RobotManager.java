package uv.simuladoraleexplorador.main.ui.view3d;
import javafx.geometry.Bounds;
import javafx.scene.Group;
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

    // --- AHORA USAMOS LISTAS PARA MUCHOS OBJETOS ---
    private final List<RigidBody> bodies = new ArrayList<>();
    private final List<TransformGizmo> gizmos = new ArrayList<>();

    // Mantenemos referencia al ÚLTIMO creado por si queremos controlarlo específicamente
    private RigidBody activeBody;
    private TransformGizmo activeGizmo;

    public RobotManager(Group worldGroup, PhysicsEngine physics, World3D worldRef) {
        this.worldGroup = worldGroup;
        this.physics = physics;
        this.worldRef = worldRef;
    }

    // Método para importar CAD (.obj)
    public RigidBody spawnRobot(Group nuevoModelo) {
        // NOTA: YA NO LLAMAMOS A cleanOldRobot() AQUÍ. ¡Queremos acumular!

        // 1. Bounds y Centrado (Igual que antes)
        Bounds bounds = nuevoModelo.getBoundsInParent();
        float realWidth = Math.max(1.0f, (float) bounds.getWidth());
        float realHeight = Math.max(1.0f, (float) bounds.getHeight());
        float realDepth = Math.max(1.0f, (float) bounds.getDepth());

        Group robotActor = new Group();
        double centerX = bounds.getMinX() + (bounds.getWidth() / 2);
        double centerY = bounds.getMinY() + (bounds.getHeight() / 2);
        double centerZ = bounds.getMinZ() + (bounds.getDepth() / 2);

        nuevoModelo.setTranslateX(-centerX);
        nuevoModelo.setTranslateY(-centerY);
        nuevoModelo.setTranslateZ(-centerZ);
        robotActor.getChildren().add(nuevoModelo);
        robotActor.setUserData(ShapeType.BOX);

        createDebugBox(robotActor, realWidth, realHeight, realDepth);

        // Posicionar (Spawn un poco aleatorio para que no caigan uno encima de otro)
        double randomX = (Math.random() * 20) - 10;
        double randomZ = (Math.random() * 20) - 10;
        double alturaSpawn = -50 - (realHeight / 2);

        robotActor.setTranslateX(randomX);
        robotActor.setTranslateY(alturaSpawn);
        robotActor.setTranslateZ(randomZ);

        worldGroup.getChildren().add(robotActor);

        // Física
        activeBody = physics.addBoxBody(robotActor, 10.0f, realWidth, realHeight, realDepth);
        bodies.add(activeBody); // ¡A la lista!

        // Gizmo
        activeGizmo = new TransformGizmo(robotActor, worldRef);
        worldGroup.getChildren().add(activeGizmo);
        gizmos.add(activeGizmo);

        return activeBody;
    }

    // Método para Primitivas (Cubos, Esferas del catálogo)
    public RigidBody spawnPrimitive(Group model, ShapeType type, double sizeDim1, double sizeDim2) {
        // NO LIMPIAR. ACUMULAR.

        Group actor = new Group();
        actor.getChildren().add(model);


        // Spawn aleatorio pequeño para que no se encimen
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
            case RAMP: // <--- NUEVO CASO
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

        return activeBody;
    }

    // --- NUEVO MÉTODO PARA BORRAR TODO (SOLO CUANDO EL USUARIO QUIERA) ---
    public void clearScene() {
        // 1. Borrar Físicas
        for (RigidBody b : bodies) {
            physics.removeBody(b);
        }
        bodies.clear();
        gizmos.clear();
        activeBody = null;
        activeGizmo = null;

        // 2. Borrar Gráficos (Mantenemos el index 0 que es el suelo)
        while (worldGroup.getChildren().size() > 1) {
            worldGroup.getChildren().remove(1);
        }
        System.out.println(">> Escena Limpiada Completamente");
    }

    // --- NUEVO: RESETEAR POSICIONES DE TODOS ---
    public void resetAllPositions() {
        for (RigidBody b : bodies) {
            resetOneBody(b);
        }
        physics.updateGraphics(); // Forzar actualización visual
    }

    private void resetOneBody(RigidBody b) {
        if (b == null) return;
        com.bulletphysics.linearmath.Transform t = new com.bulletphysics.linearmath.Transform();
        t.setIdentity();
        b.getWorldTransform(t);
        t.origin.y = -80f; // Subirlos al cielo

        b.setWorldTransform(t);
        b.setLinearVelocity(new javax.vecmath.Vector3f(0,0,0));
        b.setAngularVelocity(new javax.vecmath.Vector3f(0,0,0));
        b.activate();
    }

    private void createDebugBox(Group actor, float w, float h, float d) {
        Box debugBox = new Box(w, h, d);
        debugBox.setMaterial(new PhongMaterial(Color.RED));
        debugBox.setDrawMode(DrawMode.LINE);
        debugBox.setMouseTransparent(true);
        actor.getChildren().add(debugBox);
    }

    public TransformGizmo getLastGizmo() {
        return activeGizmo;
    }
    public void updateAllGizmos() {
        for (TransformGizmo g : gizmos) {
            g.updatePosition();
        }
    }
}