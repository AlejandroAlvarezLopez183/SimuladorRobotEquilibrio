package uv.simuladoraleexplorador.main.model.physics;

import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import javax.vecmath.Vector3f;
import javafx.scene.Node;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.CylinderShape;

public class RigidBodyFactory {

    public static RigidBody createGround() {
        // Suelo en Y=0, apuntando arriba
        CollisionShape groundShape = new StaticPlaneShape(new Vector3f(0, -1, 0), 0);
        groundShape.setMargin(0.0f); // Precisión absoluta

        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0, new DefaultMotionState(), groundShape);
        RigidBody body = new RigidBody(rbInfo);

        Transform t = new Transform();
        t.setIdentity();
        t.origin.set(0, 0, 0);
        body.setWorldTransform(t);

        body.setRestitution(0.1f);
        body.setFriction(1.0f);
        return body;
    }

    public static RigidBody createBox(Node graphicsNode, float mass, float width, float height, float depth) {
        // Crear forma
        CollisionShape shape = new BoxShape(new Vector3f(width / 2, height / 2, depth / 2));
        shape.setMargin(0.001f); // Margen mínimo para precisión visual

        // Calcular Inercia
        Vector3f localInertia = new Vector3f(0, 0, 0);
        if (mass > 0) {
            shape.calculateLocalInertia(mass, localInertia);
        }

        // Posición Inicial
        Transform startTransform = new Transform();
        startTransform.setIdentity();
        startTransform.origin.set(
                (float) graphicsNode.getTranslateX(),
                (float) graphicsNode.getTranslateY(),
                (float) graphicsNode.getTranslateZ()
        );

        // Construir cuerpo
        DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, shape, localInertia);
        RigidBody body = new RigidBody(rbInfo);

        // Propiedades materiales
        body.setRestitution(0.3f);
        body.setFriction(0.8f);
        body.setDamping(0.0f, 0.0f);
        body.activate();

        return body;
    }
    public static RigidBody createSphere(Node graphicsNode, float mass, float radius) {
        CollisionShape shape = new SphereShape(radius);
        return buildBody(graphicsNode, mass, shape);
    }

    public static RigidBody createCylinder(Node graphicsNode, float mass, float radius, float height) {
        // En JBullet el cilindro se define por (extentX, extentY, extentZ)
        CollisionShape shape = new CylinderShape(new Vector3f(radius, height / 2, radius));
        return buildBody(graphicsNode, mass, shape);
    }

    private static RigidBody buildBody(Node graphicsNode, float mass, CollisionShape shape) {
        shape.setMargin(0.001f);
        Vector3f localInertia = new Vector3f(0, 0, 0);
        if (mass > 0) shape.calculateLocalInertia(mass, localInertia);

        Transform startTransform = new Transform();
        startTransform.setIdentity();
        startTransform.origin.set(
                (float) graphicsNode.getTranslateX(),
                (float) graphicsNode.getTranslateY(),
                (float) graphicsNode.getTranslateZ()
        );

        DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, shape, localInertia);
        RigidBody body = new RigidBody(rbInfo);

        body.setRestitution(0.4f);
        body.setFriction(0.8f);
        body.activate();
        return body;
    }
}