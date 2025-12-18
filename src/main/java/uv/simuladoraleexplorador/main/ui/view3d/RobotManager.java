package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;

// Imports de JBullet
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.linearmath.Transform;

import java.util.ArrayList;
import java.util.List;

public class RobotManager {

    public enum ShapeType { BOX, SPHERE, CYLINDER, RAMP }
    private final Group worldGroup;
    private final PhysicsEngine physics;
    private final World3D worldRef;

    // Listas de gestión
    private final List<RigidBody> bodies = new ArrayList<>();
    private final List<TransformGizmo> gizmos = new ArrayList<>();
    private final List<Node> debugShapes = new ArrayList<>(); // Lista de Cajas Rojas

    // Referencias temporales
    private RigidBody activeBody;
    private TransformGizmo activeGizmo;

    // Estados de visibilidad
    private boolean areGizmosVisible = true;
    private boolean areDebugVisible = true;

    public RobotManager(Group worldGroup, PhysicsEngine physics, World3D worldRef) {
        this.worldGroup = worldGroup;
        this.physics = physics;
        this.worldRef = worldRef;
    }

    // --- SPAWN: IMPORTADOS (.OBJ) ---
    public RigidBody spawnRobot(Group nuevoModelo) {
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

        // Caja Roja Debug
        createDebugBox(robotActor, realWidth, realHeight, realDepth);

        // Posicionamiento
        double randomX = (Math.random() * 20) - 10;
        double randomZ = (Math.random() * 20) - 10;
        double alturaSpawn = -50 - (realHeight / 2);

        robotActor.setTranslateX(randomX);
        robotActor.setTranslateY(alturaSpawn);
        robotActor.setTranslateZ(randomZ);

        worldGroup.getChildren().add(robotActor);

        // Física
        activeBody = physics.addBoxBody(robotActor, 10.0f, realWidth, realHeight, realDepth);
        bodies.add(activeBody);

        // Gizmo
        createGizmoFor(robotActor);

        return activeBody;
    }

    // --- SPAWN: PRIMITIVAS ---
    public RigidBody spawnPrimitive(Group model, ShapeType type, double sizeDim1, double sizeDim2) {
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

        createGizmoFor(actor);

        return activeBody;
    }

    // --- MÉTODO PRINCIPAL DE AGRUPACIÓN (CORREGIDO Y COMPLETO) ---
    public void mergeObjectB_into_ObjectA(Node parentNode, RigidBody parentBody, Node childNode, RigidBody childBody) {
        System.out.println("Agrupando objetos...");

        // 1. Obtener formas físicas actuales
        CollisionShape shapeA = parentBody.getCollisionShape();
        CollisionShape shapeB = childBody.getCollisionShape();

        // 2. Calcular Transformaciones Mundiales
        Transform transA = new Transform();
        parentBody.getWorldTransform(transA); // Dónde está A en el mundo

        Transform transB = new Transform();
        childBody.getWorldTransform(transB);  // Dónde está B en el mundo

        // 3. Calcular Transformación Relativa (Offset = Inverse(A) * B)
        Transform offsetB = new Transform();
        offsetB.inverse(transA);
        offsetB.mul(transB);

        // 4. Preparar listas para el nuevo CompoundShape
        List<CollisionShape> shapes = new ArrayList<>();
        List<Transform> transforms = new ArrayList<>();

        // -- Caso A: El padre YA ERA un grupo --
        if (shapeA instanceof CompoundShape) {
            CompoundShape compA = (CompoundShape) shapeA;
            for (int i = 0; i < compA.getNumChildShapes(); i++) {
                shapes.add(compA.getChildShape(i));
                Transform t = new Transform();
                compA.getChildTransform(i, t);
                transforms.add(t);
            }
        } else {
            // -- Caso B: El padre era un objeto simple --
            shapes.add(shapeA);
            Transform tIdent = new Transform();
            tIdent.setIdentity(); // El padre está en el centro (0,0,0) de sí mismo
            transforms.add(tIdent);
        }

        // -- Añadir el Hijo (Objeto B) --
        shapes.add(shapeB);
        transforms.add(offsetB);

        // --- 5. GESTIÓN VISUAL (LA MUDANZA) ---
        // Si el padre es un Grupo, metemos al hijo dentro para que se muevan juntos visualmente
        if (parentNode instanceof Group) {
            Group parentGroup = (Group) parentNode;

            // Guardamos la posición mundial actual del hijo antes de moverlo
            double worldX = childNode.getTranslateX();
            double worldY = childNode.getTranslateY();
            double worldZ = childNode.getTranslateZ();

            // Quitamos el hijo del mundo (esto lo hacía desaparecer antes)
            worldGroup.getChildren().remove(childNode);

            // ¡Lo agregamos al padre!
            parentGroup.getChildren().add(childNode);

            // Ajustamos coordenadas locales: Posición Mundial - Posición del Padre
            // (Nota: Esto asume que el padre no está rotado. Si lo rotas antes de unir,
            // necesitarías usar parentGroup.sceneToLocal(...))
            childNode.setTranslateX(worldX - parentGroup.getTranslateX());
            childNode.setTranslateY(worldY - parentGroup.getTranslateY());
            childNode.setTranslateZ(worldZ - parentGroup.getTranslateZ());
        }

        // --- 6. CREAR EL NUEVO CUERPO FÍSICO ---
        float totalMass = 10.0f + 10.0f; // Sumar masas o recalcular
        RigidBody newBody = physics.addCompoundBody(parentNode, totalMass, shapes, transforms);

        // --- 7. LIMPIEZA ---
        physics.removeBody(parentBody);
        physics.removeBody(childBody);
        bodies.remove(parentBody);
        bodies.remove(childBody);
        bodies.add(newBody);

        // Gestión de Gizmos: Borrar viejos, crear nuevo para el padre
        removeGizmoFor(parentNode);
        removeGizmoFor(childNode);
        createGizmoFor((Group) parentNode);

        System.out.println("¡Objetos Agrupados y VISIBLES!");
    }

    // --- UTILIDADES DE RASTREO (Necesario para MainController) ---

    // Este método es VITAL para que funcione la Selección de Caja (Marquee)
    public List<Node> getTrackedNodes() {
        List<Node> nodes = new ArrayList<>();
        // Usamos los gizmos para saber qué objetos son interactuables en la escena
        for (TransformGizmo g : gizmos) {
            if (g.getTargetNode() != null) {
                nodes.add(g.getTargetNode());
            }
        }
        return nodes;
    }

    // --- UTILIDADES PRIVADAS ---

    private void createGizmoFor(Group target) {
        activeGizmo = new TransformGizmo(target, worldRef);
        worldGroup.getChildren().add(activeGizmo);
        gizmos.add(activeGizmo);
        activeGizmo.setVisible(areGizmosVisible);
    }

    private void removeGizmoFor(Node target) {
        gizmos.removeIf(g -> {
            if (g.getTargetNode() == target) {
                worldGroup.getChildren().remove(g);
                return true;
            }
            return false;
        });
    }

    private void createDebugBox(Group actor, float w, float h, float d) {
        Box debugBox = new Box(w, h, d);
        debugBox.setMaterial(new PhongMaterial(Color.RED));
        debugBox.setDrawMode(DrawMode.LINE);
        debugBox.setMouseTransparent(true);

        actor.getChildren().add(debugBox);
        debugShapes.add(debugBox);
        debugBox.setVisible(areDebugVisible);
    }

    // --- GESTIÓN GENERAL ---

    public void removeRobot(Node robotNode, RigidBody body) {
        bodies.remove(body);
        removeGizmoFor(robotNode);
        debugShapes.removeIf(node -> node.getParent() == robotNode);

        physics.removeBody(body);
        worldGroup.getChildren().remove(robotNode);
        System.out.println("Objeto eliminado correctamente.");
    }

    public void clearScene() {
        for (RigidBody b : bodies) physics.removeBody(b);
        bodies.clear();
        gizmos.clear();
        debugShapes.clear();

        activeBody = null;
        activeGizmo = null;

        // Mantenemos index 0 (suelo)
        while (worldGroup.getChildren().size() > 1) {
            worldGroup.getChildren().remove(1);
        }
        System.out.println(">> Escena Limpiada");
    }

    public void resetAllPositions() {
        for (RigidBody b : bodies) resetOneBody(b);
        physics.updateGraphics();
    }

    private void resetOneBody(RigidBody b) {
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

    // --- SETTERS / GETTERS ---

    public TransformGizmo getLastGizmo() { return activeGizmo; }

    public void updateAllGizmos() {
        for (TransformGizmo g : gizmos) g.updatePosition();
    }

    public void setGizmosVisible(boolean visible) {
        this.areGizmosVisible = visible;
        for (TransformGizmo g : gizmos) g.setVisible(visible);
    }

    public void setDebugVisible(boolean visible) {
        this.areDebugVisible = visible;
        for (Node box : debugShapes) {
            box.setVisible(visible);
        }
    }
}