package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

public class ObjectLibraryUI {

    private final RobotManager robotManager;
    private final VBox container;

    public ObjectLibraryUI(RobotManager robotManager) {
        this.robotManager = robotManager;
        this.container = new VBox(10);
        this.container.setPadding(new Insets(10));
        buildUI();
    }

    private void buildUI() {
        Label title = new Label("Objetos de Prueba");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button btnCube = createButton("Cubo (10u)", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createCube(10), RobotManager.ShapeType.BOX, 10, 10);
        });

        Button btnSphere = createButton("Esfera (8u)", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createSphere(8), RobotManager.ShapeType.SPHERE, 8, 0);
        });

        Button btnCylinder = createButton("Cilindro", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createCylinder(5, 15), RobotManager.ShapeType.CYLINDER, 5, 15);
        });

        // La rampa la trataremos como Caja en física por ahora para no complicar con ConvexHulls hoy
        Button btnRamp = createButton("Rampa", () -> {
            robotManager.spawnPrimitive(PrimitiveFactory.createRamp(15), RobotManager.ShapeType.RAMP, 15, 15);
        });

        container.getChildren().addAll(title, btnCube, btnSphere, btnCylinder, btnRamp);
    }

    private Button createButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    public TitledPane getView() {
        TitledPane pane = new TitledPane("Catálogo", container);
        pane.setExpanded(true);
        return pane;
    }
}