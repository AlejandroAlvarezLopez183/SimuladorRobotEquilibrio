package uv.simuladoraleexplorador.main.ui.view3d.robot;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.geometry.Point3D;
import javafx.scene.shape.Box;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.linearmath.Transform;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;
import uv.simuladoraleexplorador.main.ui.view3d.RobotManager;
import uv.simuladoraleexplorador.main.ui.view3d.TransformGizmo;
import uv.simuladoraleexplorador.main.ui.view3d.World3D;

import java.util.List;
import java.util.ArrayList;

public class RobotGrouper {

    private final PhysicsEngine physics;
    private final Group worldGroup;
    private final World3D worldRef;
    private final RobotRegistry registry;

    public RobotGrouper(PhysicsEngine physics, Group worldGroup, World3D worldRef, RobotRegistry registry) {
        this.physics = physics;
        this.worldGroup = worldGroup;
        this.worldRef = worldRef;
        this.registry = registry;
    }

    // --- AGRUPAR (GROUP) ---
    public void groupObjects(List<Node> nodesToGroup) {
        if (nodesToGroup.size() < 2) return;

        // 1. Crear Padre Visual
        Group groupParent = new Group();
        Node anchor = nodesToGroup.get(0);
        double anchorX = anchor.getTranslateX();
        double anchorY = anchor.getTranslateY();
        double anchorZ = anchor.getTranslateZ();

        groupParent.setTranslateX(anchorX);
        groupParent.setTranslateY(anchorY);
        groupParent.setTranslateZ(anchorZ);
        groupParent.setUserData(RobotManager.ShapeType.BOX);

        worldGroup.getChildren().add(groupParent);

        // 2. Calcular Física Compuesta
        CompoundShape compoundShape = new CompoundShape();
        float totalMass = 0;
        List<RigidBody> bodiesToRemove = new ArrayList<>();

        for (Node child : nodesToGroup) {
            RigidBody rb = physics.getBodyFromGraphic(child);
            if (rb == null) continue;

            // Física Relativa
            Transform childTrans = new Transform();
            rb.getWorldTransform(childTrans);
            Transform parentTrans = new Transform();
            parentTrans.setIdentity();
            parentTrans.origin.set((float)anchorX, (float)anchorY, (float)anchorZ);

            Transform offset = new Transform();
            offset.inverse(parentTrans);
            offset.mul(childTrans);

            compoundShape.addChildShape(offset, rb.getCollisionShape());
            if (rb.getInvMass() != 0) totalMass += (1.0f / rb.getInvMass());
            bodiesToRemove.add(rb);

            // Visual Relativa (Mudanza)
            // Primero quitamos del registro para limpiar gizmos viejos
            registry.unregister(rb, child);

            // Luego mudanza visual
            worldGroup.getChildren().remove(child);
            groupParent.getChildren().add(child);

            // Ajuste fino de posición local
            child.setTranslateX(childTrans.origin.x - anchorX);
            child.setTranslateY(childTrans.origin.y - anchorY);
            child.setTranslateZ(childTrans.origin.z - anchorZ);
        }

        // 3. Crear Cuerpo Nuevo
        RigidBody newBody = physics.addCompoundBody(groupParent, totalMass, compoundShape);

        // 4. Crear Gizmo Nuevo
        TransformGizmo newGizmo = new TransformGizmo(groupParent, worldRef);
        worldGroup.getChildren().add(newGizmo);

        // 5. Registrar Padre
        registry.register(newBody, newGizmo, null);

        // 6. Limpiar física vieja del motor
        for (RigidBody b : bodiesToRemove) physics.removeBody(b);

        System.out.println("Grupo creado con " + nodesToGroup.size() + " elementos.");
    }

    // --- DESAGRUPAR (UNGROUP) ---
    public void ungroupObject(Node groupNode, RigidBody groupBody) {
        if (!(groupNode instanceof Group) || !(groupBody.getCollisionShape() instanceof CompoundShape)) {
            System.out.println("No es un grupo válido para desagrupar.");
            return;
        }

        Group parentGroup = (Group) groupNode;
        List<Node> children = new ArrayList<>(parentGroup.getChildren());

        // 1. Limpiar Padre (Física y Visual)
        registry.unregister(groupBody, parentGroup);
        physics.removeBody(groupBody);
        worldGroup.getChildren().remove(parentGroup);

        // 2. Liberar Hijos
        for (Node child : children) {
            if (child instanceof Box) continue; // Ignorar debug boxes internas si las hubiera

            // A. VISUAL: Regresar al mundo
            parentGroup.getChildren().remove(child);
            worldGroup.getChildren().add(child);

            // Restaurar posición mundial absoluta
            // (Sumamos la posición del padre + la local del hijo)
            child.setTranslateX(parentGroup.getTranslateX() + child.getTranslateX());
            child.setTranslateY(parentGroup.getTranslateY() + child.getTranslateY());
            child.setTranslateZ(parentGroup.getTranslateZ() + child.getTranslateZ());

            // B. FÍSICA: Recrear cuerpo individual
            // Nota: Aquí simplificamos creando una caja por defecto.
            // Para ser exactos, deberíamos guardar el tipo original en el UserData.
            RigidBody newBody = physics.addBoxBody(child, 10f, 5,5,5);

            // C. GIZMO Y REGISTRO (Aquí estaba el error)
            // Verificamos que sea un Grupo antes de crear el Gizmo
            if (child instanceof Group) {
                // CORRECCIÓN: Casteamos (Group) child
                TransformGizmo gizmo = new TransformGizmo((Group) child, worldRef);
                worldGroup.getChildren().add(gizmo);
                registry.register(newBody, gizmo, null);
            } else {
                // Si por alguna razón es una forma suelta (raro en tu arquitectura), registramos sin gizmo o lo envolvemos
                registry.register(newBody, null, null);
            }
        }
        System.out.println("Grupo desagrupado.");
    }
}