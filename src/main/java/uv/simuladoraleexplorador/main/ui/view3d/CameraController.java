package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class CameraController {

    private double mouseOldX, mouseOldY;
    private final Rotate rotateX;
    private final Rotate rotateY;
    private final Translate translateZ;
    private final SubScene subScene;

    public CameraController(SubScene subScene, Rotate rotateX, Rotate rotateY, Translate translateZ) {
        this.subScene = subScene;
        this.rotateX = rotateX;
        this.rotateY = rotateY;
        this.translateZ = translateZ;
        initInput();
    }

    private void initInput() {
        subScene.setOnMousePressed(event -> {
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        subScene.setOnMouseDragged(event -> {
            double dx = event.getSceneX() - mouseOldX;
            double dy = event.getSceneY() - mouseOldY;

            rotateY.setAngle(rotateY.getAngle() + dx * 0.3);
            rotateX.setAngle(rotateX.getAngle() - dy * 0.3);

            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        subScene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY();
            double newZ = translateZ.getZ() + (delta * 0.5);
            if (newZ < -2 && newZ > -5000) {
                translateZ.setZ(newZ);
            }
        });
    }
}