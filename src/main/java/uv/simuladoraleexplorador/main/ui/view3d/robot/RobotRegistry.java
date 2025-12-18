package uv.simuladoraleexplorador.main.ui.view3d.robot;

import javafx.scene.Node;
import javafx.scene.Group;
import com.bulletphysics.dynamics.RigidBody;
import uv.simuladoraleexplorador.main.ui.view3d.TransformGizmo;
import java.util.ArrayList;
import java.util.List;

public class RobotRegistry {

    // Las listas sagradas
    private final List<RigidBody> bodies = new ArrayList<>();
    private final List<TransformGizmo> gizmos = new ArrayList<>();
    private final List<Node> debugShapes = new ArrayList<>();

    // --- GESTIÓN BÁSICA ---
    public void register(RigidBody body, TransformGizmo gizmo, Node debugBox) {
        if (body != null) bodies.add(body);
        if (gizmo != null) gizmos.add(gizmo);
        if (debugBox != null) debugShapes.add(debugBox);
    }

    public void unregister(RigidBody body, Node visualNode) {
        bodies.remove(body);

        // Eliminar Gizmo asociado al nodo
        gizmos.removeIf(g -> {
            boolean match = (g.getTargetNode() == visualNode);
            if (match) {
                // Truco: Desconectar visualmente aquí o en el Manager
                if (g.getParent() instanceof Group) ((Group)g.getParent()).getChildren().remove(g);
            }
            return match;
        });

        // Eliminar DebugBox asociada (hija del nodo)
        debugShapes.removeIf(n -> n.getParent() == visualNode);
    }

    public void clearAll() {
        bodies.clear();
        gizmos.clear();
        debugShapes.clear();
    }

    // --- GETTERS PARA LÓGICA EXTERNA ---
    public List<RigidBody> getBodies() { return bodies; }
    public List<TransformGizmo> getGizmos() { return gizmos; }
    public List<Node> getDebugShapes() { return debugShapes; }

    // Vital para la selección de caja azul
    public List<Node> getTrackedNodes() {
        List<Node> nodes = new ArrayList<>();
        for (TransformGizmo g : gizmos) {
            if (g.getTargetNode() != null) nodes.add(g.getTargetNode());
        }
        return nodes;
    }
}