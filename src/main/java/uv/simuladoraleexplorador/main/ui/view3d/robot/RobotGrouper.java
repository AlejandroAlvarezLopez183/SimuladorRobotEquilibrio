package uv.simuladoraleexplorador.main.ui.view3d.robot;

import javafx.scene.Group;
import javafx.scene.Node;
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

        // --- PASO 1: CALCULAR EL CENTRO REAL (CENTROIDE) ---
        // Esto evita que el grupo gire raro o que el tercer objeto se sienta "pesado"
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        // Calculamos la caja que envuelve a todos los objetos
        for (Node n : nodesToGroup) {
            double x = n.getTranslateX();
            double y = n.getTranslateY();
            double z = n.getTranslateZ();
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }

        // El centro del grupo será el centro de esa caja envolvente
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        // --- PASO 2: CREAR PADRE VISUAL EN EL CENTRO ---
        Group groupParent = new Group();
        groupParent.setTranslateX(centerX);
        groupParent.setTranslateY(centerY);
        groupParent.setTranslateZ(centerZ);

        // ¡TRUCO! Marcamos esto como "COMPOUND" (Compuesto) para que el Editor sepa qué hacer
        // (Aunque usaremos un truco en el editor para no cambiar el Enum todavía)
        groupParent.setUserData("GRUPO");

        worldGroup.getChildren().add(groupParent);

        // --- PASO 3: MUDANZA FÍSICA Y VISUAL ---
        CompoundShape compoundShape = new CompoundShape();
        float totalMass = 0;
        List<RigidBody> bodiesToRemove = new ArrayList<>();

        for (Node child : nodesToGroup) {
            RigidBody rb = physics.getBodyFromGraphic(child);
            if (rb == null) continue;

            // A. Física Relativa al NUEVO CENTRO
            Transform childTrans = new Transform();
            rb.getWorldTransform(childTrans);

            Transform parentTrans = new Transform();
            parentTrans.setIdentity();
            parentTrans.origin.set((float)centerX, (float)centerY, (float)centerZ);

            Transform offset = new Transform();
            offset.inverse(parentTrans);
            offset.mul(childTrans);

            compoundShape.addChildShape(offset, rb.getCollisionShape());
            if (rb.getInvMass() != 0) totalMass += (1.0f / rb.getInvMass());
            bodiesToRemove.add(rb);

            // B. Visual Relativa
            registry.unregister(rb, child);
            child.setUserData(null); // Quitar identidad para que seleccionemos el grupo

            worldGroup.getChildren().remove(child);
            groupParent.getChildren().add(child);

            // Ajuste: Posición actual MENOS el nuevo centro
            child.setTranslateX(childTrans.origin.x - centerX);
            child.setTranslateY(childTrans.origin.y - centerY);
            child.setTranslateZ(childTrans.origin.z - centerZ);
        }

        // 4. Finalizar
        RigidBody newBody = physics.addCompoundBody(groupParent, totalMass, compoundShape);
        TransformGizmo newGizmo = new TransformGizmo(groupParent, worldRef);
        worldGroup.getChildren().add(newGizmo);
        registry.register(newBody, newGizmo, null);

        // 5. Limpiar viejos
        for (RigidBody b : bodiesToRemove) physics.removeBody(b);

        System.out.println("Grupo creado con centro equilibrado en: " + centerX + ", " + centerY + ", " + centerZ);
    }

    // --- DESAGRUPAR (UNGROUP) ---
    public void ungroupObject(Node groupNode, RigidBody groupBody) {
        if (!(groupNode instanceof Group) || !(groupBody.getCollisionShape() instanceof CompoundShape)) {
            return;
        }

        Group parentGroup = (Group) groupNode;
        List<Node> children = new ArrayList<>(parentGroup.getChildren());

        // 1. Limpiar Padre
        registry.unregister(groupBody, parentGroup);
        physics.removeBody(groupBody);
        worldGroup.getChildren().remove(parentGroup);

        // 2. Liberar Hijos
        for (Node child : children) {
            if (child instanceof Box && ((Box)child).getMaterial() == null) continue; // Ignorar artefactos

            // A. VISUAL: Regresar al mundo
            parentGroup.getChildren().remove(child);
            worldGroup.getChildren().add(child);

            // Restaurar posición mundial
            child.setTranslateX(parentGroup.getTranslateX() + child.getTranslateX());
            child.setTranslateY(parentGroup.getTranslateY() + child.getTranslateY());
            child.setTranslateZ(parentGroup.getTranslateZ() + child.getTranslateZ());

            // B. ¡CORRECCIÓN CLAVE! Devolverles la identidad.
            // Al desagrupar, vuelven a ser objetos seleccionables.
            // (Por simplicidad asumimos BOX, idealmente guardarías el tipo original en un mapa temporal)
            child.setUserData(RobotManager.ShapeType.BOX);

            // C. FÍSICA: Recrear cuerpo individual
            RigidBody newBody = physics.addBoxBody(child, 10f, 5,5,5);
            // Nota: Aquí se pierde la forma original (esfera/cilindro) al desagrupar porque PhysicsEngine.addBoxBody fuerza una caja.
            // Para arreglar esto 100% necesitarías guardar metadata del tipo en cada hijo antes de borrar el UserData.

            // D. GIZMO Y REGISTRO
            TransformGizmo gizmo = new TransformGizmo((Group)child, worldRef); // Asumimos que los hijos eran Groups (PrimitiveFactory crea Groups)
            worldGroup.getChildren().add(gizmo);
            registry.register(newBody, gizmo, null);
        }
        System.out.println("Grupo desagrupado.");
    }
}