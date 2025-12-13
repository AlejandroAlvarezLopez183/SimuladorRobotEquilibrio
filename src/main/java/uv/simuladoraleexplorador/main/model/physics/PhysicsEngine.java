package uv.simuladoraleexplorador.main.model.physics;

import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f; // Importante para la rotación
import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Point3D;
import javafx.scene.Node;

public class PhysicsEngine {

    private DiscreteDynamicsWorld dynamicsWorld;
    private final Map<RigidBody, Node> physicsToGraphicsMap = new HashMap<>();

    // --- VARIABLES RECICLABLES (OPTIMIZACIÓN) ---
    // Las creamos AQUÍ una sola vez para no estresar al Garbage Collector
    private final Transform tempTrans = new Transform();
    private final Quat4f tempRot = new Quat4f();
    private final AxisAngle4f tempAxisAngle = new AxisAngle4f();
    private final Transform auxTrans = new Transform(); // Auxiliar para updates manuales
    // ---------------------------------------------

    public PhysicsEngine() {
        initPhysics();
    }

    private void initPhysics() {
        DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        DbvtBroadphase broadphase = new DbvtBroadphase();
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3f(0, 9.81f, 0));

        // Suelo
        RigidBody groundBody = RigidBodyFactory.createGround();
        dynamicsWorld.addRigidBody(groundBody);
    }

    public RigidBody addBoxBody(Node graphicsNode, float mass, float width, float height, float depth) {
        RigidBody body = RigidBodyFactory.createBox(graphicsNode, mass, width, height, depth);
        dynamicsWorld.addRigidBody(body);
        physicsToGraphicsMap.put(body, graphicsNode);
        return body;
    }

    public void removeBody(RigidBody body) {
        if (body != null) {
            dynamicsWorld.removeRigidBody(body);
            physicsToGraphicsMap.remove(body);
            body.destroy();
        }
    }

    public void stepSimulation(float deltaTime) {
        // Optimización JBullet: maxSubSteps en 10 ayuda a mantener estabilidad si baja el FPS
        dynamicsWorld.stepSimulation(deltaTime, 10);
        updateGraphics();
    }

    // --- AQUÍ ESTÁ LA MAGIA DE LA OPTIMIZACIÓN ---
    public void updateGraphics() {
        for (Map.Entry<RigidBody, Node> entry : physicsToGraphicsMap.entrySet()) {
            RigidBody rb = entry.getKey();
            Node node = entry.getValue();

            // Usamos 'tempTrans' (reciclada) en vez de 'new Transform()'
            rb.getMotionState().getWorldTransform(tempTrans);

            // Posición
            node.setTranslateX(tempTrans.origin.x);
            node.setTranslateY(tempTrans.origin.y);
            node.setTranslateZ(tempTrans.origin.z);

            // Rotación
            tempTrans.getRotation(tempRot); // Usamos tempRot reciclada
            tempAxisAngle.set(tempRot);     // Usamos tempAxisAngle reciclada

            node.setRotationAxis(new Point3D(tempAxisAngle.x, tempAxisAngle.y, tempAxisAngle.z));
            node.setRotate(Math.toDegrees(tempAxisAngle.angle));
        }
    }

    public void updatePhysicsFromGraphicPosition(Node graphicNode) {
        RigidBody body = null;
        for (Map.Entry<RigidBody, Node> entry : physicsToGraphicsMap.entrySet()) {
            if (entry.getValue() == graphicNode) {
                body = entry.getKey();
                break;
            }
        }

        if (body != null) {
            auxTrans.setIdentity(); // Limpiamos la auxiliar
            auxTrans.origin.set(
                    (float) graphicNode.getTranslateX(),
                    (float) graphicNode.getTranslateY(),
                    (float) graphicNode.getTranslateZ()
            );

            // Mantener rotación actual reciclando tempTrans
            body.getWorldTransform(tempTrans);
            auxTrans.setRotation(tempTrans.getRotation(tempRot));

            body.setWorldTransform(auxTrans);

            body.setLinearVelocity(new Vector3f(0,0,0)); // Vector3f es ligero, este se puede quedar o reciclar si quieres extremo
            body.setAngularVelocity(new Vector3f(0,0,0));
            body.activate();
        }
    }

    public void updateBodyProperties(RigidBody body, float newMass, float newDamping) {
        if (body == null) return;

        // 1. Calcular nueva inercia (si cambia el peso, cambia cómo se mueve)
        javax.vecmath.Vector3f localInertia = new javax.vecmath.Vector3f(0, 0, 0);
        if (newMass > 0) {
            body.getCollisionShape().calculateLocalInertia(newMass, localInertia);
        }
        // 2. Aplicar cambios al motor físico
        body.setMassProps(newMass, localInertia);
        // Damping = Resistencia al aire (0.0 = vacío, 1.0 = melaza)
        // El segundo parámetro es damping angular (giro), lo dejamos en 0.5 por defecto
        body.setDamping(newDamping, 0.5f);

        body.updateInertiaTensor(); // Recalcular matemáticas internas
        body.activate();
    }
    public RigidBody addSphereBody(Node graphicsNode, float mass, float radius) {
        RigidBody body = RigidBodyFactory.createSphere(graphicsNode, mass, radius);
        dynamicsWorld.addRigidBody(body);
        physicsToGraphicsMap.put(body, graphicsNode);
        return body;
    }

    public RigidBody addCylinderBody(Node graphicsNode, float mass, float radius, float height) {
        RigidBody body = RigidBodyFactory.createCylinder(graphicsNode, mass, radius, height);
        dynamicsWorld.addRigidBody(body);
        physicsToGraphicsMap.put(body, graphicsNode);
        return body;
    }
}