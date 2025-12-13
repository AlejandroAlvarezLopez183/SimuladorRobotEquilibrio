package uv.simuladoraleexplorador.main.model.physics;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Point3D;
import javafx.scene.Node;

public class PhysicsEngine {

    private DiscreteDynamicsWorld dynamicsWorld;
    private final Map<RigidBody, Node> physicsToGraphicsMap = new HashMap<>();

    public PhysicsEngine() {
        initPhysics();
    }

    private void initPhysics() {
        // Configuración JBullet estándar
        DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        DbvtBroadphase broadphase = new DbvtBroadphase();
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3f(0, 9.81f, 0));

        // USAMOS LA FÁBRICA NUEVA:
        RigidBody groundBody = RigidBodyFactory.createGround();
        dynamicsWorld.addRigidBody(groundBody);
    }

    private void createGround() {
        // --- CALIBRACIÓN DE SUELO ---
        // 1. Vector (0, -1, 0): Apunta hacia ARRIBA en JavaFX (donde Y es abajo)
        // 2. Constante 0: Lo pone exactamente en el origen Y=0
        CollisionShape groundShape = new StaticPlaneShape(new Vector3f(0, -1, 0), 0);
        groundShape.setMargin(0.0f);

        RigidBodyConstructionInfo groundRigidBodyCI = new RigidBodyConstructionInfo(0, new DefaultMotionState(), groundShape);
        RigidBody groundBody = new RigidBody(groundRigidBodyCI);

        // Ubicarlo en 0,0,0
        com.bulletphysics.linearmath.Transform groundTransform = new com.bulletphysics.linearmath.Transform();
        groundTransform.setIdentity();
        groundTransform.origin.set(0, 0, 0);
        groundBody.setWorldTransform(groundTransform);

        // Material del suelo (Poco rebote, mucha fricción)
        groundBody.setRestitution(0.1f);
        groundBody.setFriction(1.0f);

        dynamicsWorld.addRigidBody(groundBody);
    }

    public RigidBody addBoxBody(Node graphicsNode, float mass, float width, float height, float depth) {
        // USAMOS LA FÁBRICA NUEVA:
        RigidBody body = RigidBodyFactory.createBox(graphicsNode, mass, width, height, depth);

        dynamicsWorld.addRigidBody(body);
        physicsToGraphicsMap.put(body, graphicsNode);
        return body;
    }

    public void updateBodyProperties(RigidBody body, float newMass, float newDamping) {
        if (body == null) return;

        Vector3f localInertia = new Vector3f(0, 0, 0);
        if (newMass > 0) {
            body.getCollisionShape().calculateLocalInertia(newMass, localInertia);
        }

        body.setMassProps(newMass, localInertia);
        body.setDamping(newDamping, 0.0f);
        body.updateInertiaTensor();
        body.activate();
    }

    public void stepSimulation(float deltaTime) {
        dynamicsWorld.stepSimulation(deltaTime, 10);
        updateGraphics();
    }

    public void updateGraphics() {
        for (Map.Entry<RigidBody, Node> entry : physicsToGraphicsMap.entrySet()) {
            RigidBody rb = entry.getKey();
            Node node = entry.getValue();

            // Obtener transformación física
            Transform trans = new Transform();
            rb.getMotionState().getWorldTransform(trans);

            // Sincronizar Posición
            node.setTranslateX(trans.origin.x);
            node.setTranslateY(trans.origin.y);
            node.setTranslateZ(trans.origin.z);

            // Sincronizar Rotación
            Quat4f rotQuat = new Quat4f();
            trans.getRotation(rotQuat);

            javax.vecmath.AxisAngle4f axisAngle = new javax.vecmath.AxisAngle4f();
            axisAngle.set(rotQuat);

            node.setRotationAxis(new Point3D(axisAngle.x, axisAngle.y, axisAngle.z));
            node.setRotate(Math.toDegrees(axisAngle.angle));
        }
    } // <--- ¡ESTA ERA LA LLAVE QUE FALTABA!

    // Ahora este método está afuera, como debe ser
    public void updatePhysicsFromGraphicPosition(Node graphicNode) {
        RigidBody body = null;
        // Buscar qué cuerpo físico corresponde a este gráfico
        for (Map.Entry<RigidBody, Node> entry : physicsToGraphicsMap.entrySet()) {
            if (entry.getValue() == graphicNode) {
                body = entry.getKey();
                break;
            }
        }

        if (body != null) {
            // Crear nueva transformación basada en dónde puso el usuario el gráfico
            Transform newTrans = new Transform();
            newTrans.setIdentity();
            newTrans.origin.set(
                    (float) graphicNode.getTranslateX(),
                    (float) graphicNode.getTranslateY(),
                    (float) graphicNode.getTranslateZ()
            );

            // MANTENER LA ROTACIÓN ACTUAL
            Transform currentTrans = new Transform();
            body.getWorldTransform(currentTrans);
            newTrans.setRotation(currentTrans.getRotation(new Quat4f()));

            // Aplicar al cuerpo físico
            body.setWorldTransform(newTrans);

            // Resetear velocidades
            body.setLinearVelocity(new Vector3f(0,0,0));
            body.setAngularVelocity(new Vector3f(0,0,0));
            body.activate();
        }
    }
    public void removeBody(RigidBody body) {
        if (body != null) {
            // 1. Quitar del mundo físico (JBullet)
            dynamicsWorld.removeRigidBody(body);

            // 2. Quitar del mapa de sincronización
            physicsToGraphicsMap.remove(body);

            // 3. Liberar memoria (opcional pero recomendado)
            body.destroy();
        }
    }
}