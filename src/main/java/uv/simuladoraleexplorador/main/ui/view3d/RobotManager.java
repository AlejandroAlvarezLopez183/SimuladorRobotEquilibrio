package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.scene.Group;
import javafx.scene.Node;
import com.bulletphysics.dynamics.RigidBody;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;
import uv.simuladoraleexplorador.main.ui.view3d.robot.RobotGrouper;
import uv.simuladoraleexplorador.main.ui.view3d.robot.RobotRegistry;
import uv.simuladoraleexplorador.main.ui.view3d.robot.RobotSpawner;
import uv.simuladoraleexplorador.main.utils.ObjLoader;
import uv.simuladoraleexplorador.main.model.components.ComponentType;

import java.io.File;
import java.util.List;

public class RobotManager {

    public enum ShapeType { BOX, SPHERE, CYLINDER, RAMP }

    // Dependencias
    private final PhysicsEngine physics;
    private final Group worldGroup;

    // Sub-Sistemas (Delegados)
    private final RobotRegistry registry;
    private final RobotSpawner spawner;
    private final RobotGrouper grouper;

    public RobotManager(Group worldGroup, PhysicsEngine physics, World3D worldRef) {
        this.worldGroup = worldGroup;
        this.physics = physics;

        // Inicializar subsistemas
        this.registry = new RobotRegistry();
        this.spawner = new RobotSpawner(physics, worldGroup, worldRef, registry);
        this.grouper = new RobotGrouper(physics, worldGroup, worldRef, registry);
    }

    // --- DELEGACIÓN DE CREACIÓN ---
    public RigidBody spawnRobot(Group model) {
        RigidBody body = spawner.spawnImported(model);
        refreshVisuals();
        return body;
    }

    public RigidBody spawnPrimitive(Group model, ShapeType type, double s1, double s2) {
        ElectronicComponent dummyComp = new ElectronicComponent("Objeto Primitivo", uv.simuladoraleexplorador.main.model.components.ComponentType.LED);
        model.setUserData(dummyComp);
        RigidBody body = spawner.spawnPrimitive(model, type, s1, s2);
        refreshVisuals();
        return body;
    }

    // --- DELEGACIÓN DE GRUPOS ---
    public void groupObjects(List<Node> nodes) {
        grouper.groupObjects(nodes);
        refreshVisuals();
    }

    public void ungroupObject(Node node, RigidBody body) {
        grouper.ungroupObject(node, body);
        refreshVisuals();
    }

    // --- GESTIÓN GENERAL (USANDO REGISTRY) ---
    public void removeRobot(Node node, RigidBody body) {
        registry.unregister(body, node);
        physics.removeBody(body);
        worldGroup.getChildren().remove(node);
        System.out.println("Objeto eliminado.");
    }

    public void clearScene() {
        for (RigidBody b : registry.getBodies()) {
            physics.removeBody(b);
        }
        registry.clearAll();

        // Limpieza visual (Mantenemos suelo index 0)
        while (worldGroup.getChildren().size() > 1) {
            worldGroup.getChildren().remove(1);
        }
        System.out.println("Escena Limpiada.");
    }

    public void resetAllPositions() {
        for (RigidBody b : registry.getBodies()) {
            com.bulletphysics.linearmath.Transform t = new com.bulletphysics.linearmath.Transform();
            t.setIdentity();
            b.getWorldTransform(t);
            t.origin.y = -80f; // Al cielo
            b.setWorldTransform(t);
            b.setLinearVelocity(new javax.vecmath.Vector3f(0,0,0));
            b.setAngularVelocity(new javax.vecmath.Vector3f(0,0,0));
            b.activate();
        }
        physics.updateGraphics();
    }

    // --- VISIBILIDAD ---
    private boolean gizmosVisible = true;
    private boolean debugVisible = true;

    public void setGizmosVisible(boolean visible) {
        this.gizmosVisible = visible;
        refreshVisuals();
    }

    public void setDebugVisible(boolean visible) {
        this.debugVisible = visible;
        refreshVisuals();
    }

    private void refreshVisuals() {
        for (TransformGizmo g : registry.getGizmos()) g.setVisible(gizmosVisible);
        for (Node n : registry.getDebugShapes()) n.setVisible(debugVisible);
    }

    // --- ACCESO EXTERNO ---
    public List<Node> getTrackedNodes() {
        return registry.getTrackedNodes();
    }
    public uv.simuladoraleexplorador.main.ui.view3d.TransformGizmo getLastGizmo() {
        // Obtenemos la lista del registro y devolvemos el último elemento
        java.util.List<TransformGizmo> list = registry.getGizmos();
        if (list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }

    public void updateAllGizmos() {
        // Necesario para que las flechas sigan al objeto físico en cada frame
        for (TransformGizmo g : registry.getGizmos()) {
            g.updatePosition();
        }
    }
    public void spawnComponent(ElectronicComponent component) {
        Group visualModel;
        String path = component.getModelPath();
        File file = new File(path);

        // 1. Cargar Modelo (igual que antes)
        if (file.exists() && file.isFile() && path.toLowerCase().endsWith(".obj")) {
            try {
                visualModel = ObjLoader.loadModel(file);
            } catch (Exception e) {
                visualModel = createPlaceholder(component);
            }
        } else {
            visualModel = createPlaceholder(component);
        }

        visualModel.setUserData(component);

        // 2. Instanciar Físicamente (Esto crea el RigidBody con masa por defecto)
        com.bulletphysics.dynamics.RigidBody body = spawner.spawnImported(visualModel);

        if (body != null) {
            float mass = (float) component.getMass();

            // --- LÓGICA SIMPLIFICADA "ESTILO ARCADE" ---
            // Esto garantiza que lo pesado NUNCA sea más lento que lo ligero.

            float airResistance;

            if (mass >= 1.0f) {
                // PESADOS: Caen como piedra (Sin freno)
                airResistance = 0.0f;
            } else if (mass >= 0.1f) {
                // MEDIANOS: Caen normal (Freno leve)
                airResistance = 0.1f;
            } else {
                // LIGEROS: Caen como papel (Mucho freno)
                airResistance = 0.8f;
            }

            // Aplicamos cambios
            physics.updateBodyProperties(body, mass, airResistance);

            // Forzar activación para asegurar que JBullet despierte al objeto
            body.activate(true);

            System.out.println("Spawned: " + component.getName()
                    + " | Masa: " + mass
                    + " | Freno: " + airResistance);
        }

        refreshVisuals();
    }

    // Un ayudante para crear una caja visual si no hay modelo 3D
    private Group createPlaceholder(ElectronicComponent comp) {
        // Creamos un cubo pequeño (5 unidades)
        Group cube = PrimitiveFactory.createCube(5);
        // Podrías cambiarle el color según el tipo (Motor=Rojo, Sensor=Azul) si quisieras
        return cube;
    }

    public java.util.List<ElectronicComponent> getAllComponents() {
        java.util.List<ElectronicComponent> list = new java.util.ArrayList<>();

        // 1. Obtenemos todos los nodos 3D (Cajas, Esferas, Robots importados)
        // Usamos el método que ya tienes expuesto: getTrackedNodes()
        for (Node node : getTrackedNodes()) {

            // 2. Verificamos si el nodo tiene "datos de usuario" (UserData)
            Object data = node.getUserData();

            // 3. Si esos datos son un Componente Electrónico, lo agregamos a la lista
            if (data instanceof ElectronicComponent) {
                list.add((ElectronicComponent) data);
            }
        }

        return list;
    }

    public javafx.scene.Node getNodeFromComponent(ElectronicComponent comp) {
        // Busamos en todos los nodos visuales
        for (javafx.scene.Node node : getTrackedNodes()) {
            // Si el nodo tiene guardado este componente en su "UserData"
            if (node.getUserData() == comp) {
                return node; // ¡Encontrado! Devolvemos el nodo 3D (para saber su X,Y,Z)
            }
        }
        return null; // No se encontró (tal vez fue borrado)
    }

    public void spawnFromSave(uv.simuladoraleexplorador.main.model.components.save.SavedObject saved) {
        // 1. Recuperar el archivo .obj original
        File file = new File(saved.modelPath);
        Group visualModel;

        // Intentamos cargar el modelo 3D original
        if (file.exists() && saved.modelPath.toLowerCase().endsWith(".obj")) {
            try {
                visualModel = ObjLoader.loadModel(file);
                // IMPORTANTE: Si al importar rotas 90 grados, aquí también deberías hacerlo si es necesario
                // visualModel.getTransforms().add(new javafx.scene.transform.Rotate(90, javafx.scene.transform.Rotate.X_AXIS));
            } catch (Exception e) {
                System.err.println("No se pudo cargar modelo: " + saved.modelPath);
                visualModel = createPlaceholder(null); // Cubo de emergencia
            }
        } else {
            // Si el archivo ya no existe (lo borraste o moviste), creamos un cubo
            visualModel = createPlaceholder(null);
        }

        // 2. Recrear la identidad del componente (Nombre y Tipo)
        uv.simuladoraleexplorador.main.model.components.ComponentType type =
                uv.simuladoraleexplorador.main.model.components.ComponentType.valueOf(saved.type);

        ElectronicComponent comp = new ElectronicComponent(saved.id, type);
        comp.setModelPath(saved.modelPath); // Recordar dónde estaba

        visualModel.setUserData(comp);

        // 3. Crear el Cuerpo Físico (RigidBody)
        // Usamos spawnImported porque ya traemos el modelo listo
        RigidBody body = spawner.spawnImported(visualModel);

        // 4. Moverlo a la posición guardada
        if (body != null) {
            com.bulletphysics.linearmath.Transform t = new com.bulletphysics.linearmath.Transform();
            t.setIdentity();

            // Aplicar posición (X, Y, Z)
            t.origin.set((float)saved.posX, (float)saved.posY, (float)saved.posZ);

            // (Opcional) Aplicar rotación si la guardaste
            // Si guardaste rotX/Y/Z en grados, aquí habría que convertir a Cuaternión.
            // Por ahora solo restauramos posición para que aparezcan.

            body.setWorldTransform(t);

            // Restaurar Masa y Físicas
            physics.updateBodyProperties(body, saved.mass, 0.5f); // 0.5f es fricción default

            // Forzar actualización física
            body.setLinearVelocity(new javax.vecmath.Vector3f(0,0,0));
            body.setAngularVelocity(new javax.vecmath.Vector3f(0,0,0));
            body.activate(true);
        }

        refreshVisuals();
        System.out.println("📦 Objeto restaurado: " + saved.id);
    }
}