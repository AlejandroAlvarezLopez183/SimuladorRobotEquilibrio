package uv.simuladoraleexplorador.main.ui.view3d.visuals;

import javafx.scene.Group;
import uv.simuladoraleexplorador.main.model.components.logic.Wire;
import uv.simuladoraleexplorador.main.model.components.logic.WiringManager;
import uv.simuladoraleexplorador.main.ui.view3d.RobotManager;

import java.util.ArrayList;
import java.util.List;

public class CableRenderer {

    private final Group worldGroup; // Donde dibujaremos los cables
    private final RobotManager robotManager;
    private final List<VisualWire> activeVisuals = new ArrayList<>();

    public CableRenderer(Group worldGroup, RobotManager robotManager) {
        this.worldGroup = worldGroup;
        this.robotManager = robotManager;
    }

    // Llama a esto cuando agregues o borres un cable
    public void rebuildCables() {
        // 1. Limpiar visuales viejos
        for (VisualWire vw : activeVisuals) {
            worldGroup.getChildren().remove(vw.getShape());
        }
        activeVisuals.clear();

        // 2. Crear visuales nuevos basados en la lógica
        List<Wire> logicalWires = WiringManager.getInstance().getConnections();

        for (Wire wire : logicalWires) {
            // Buscamos los nodos 3D de los componentes
            javafx.scene.Node n1 = robotManager.getNodeFromComponent(wire.getSourceComponent());
            javafx.scene.Node n2 = robotManager.getNodeFromComponent(wire.getTargetComponent());

            if (n1 != null && n2 != null) {
                VisualWire visual = new VisualWire(n1, n2);
                activeVisuals.add(visual);
                worldGroup.getChildren().add(visual.getShape());
                visual.update(); // Primera actualización instantánea
            }
        }
    }

    // Llama a esto en tu AnimationTimer (bucle de juego)
    public void updateAnimation() {
        for (VisualWire vw : activeVisuals) {
            vw.update();
        }
    }
}
