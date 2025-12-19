package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;

public class SelectionVisualizer {

    private Node currentSelectionBox = null;

    public void attach(Node node) {
        // Primero limpiar lo viejo
        detach();

        if (node instanceof Group) {
            Group group = (Group) node;
            javafx.geometry.Bounds b = group.getBoundsInLocal();

            double padding = 0.2;
            Box wireBox = new Box(b.getWidth() + padding, b.getHeight() + padding, b.getDepth() + padding);

            wireBox.setDrawMode(DrawMode.LINE);
            wireBox.setMaterial(new PhongMaterial(Color.CYAN));
            wireBox.setMouseTransparent(true);

            group.getChildren().add(wireBox);
            currentSelectionBox = wireBox;
        }
    }

    public void detach() {
        if (currentSelectionBox != null) {
            Group parent = (Group) currentSelectionBox.getParent();
            if (parent != null) {
                parent.getChildren().remove(currentSelectionBox);
            }
            currentSelectionBox = null;
        }
    }

    public void update(Node node) {
        // Truco simple: quitar y volver a poner para recalcular tamaño
        detach();
        attach(node);
    }
}