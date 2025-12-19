package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;

public class EnvironmentBuilder {

    public static void build(Group root3D, Group worldGroup) {
        // 1. Suelo y Rejilla
        Group workplane = createWorkplane(1000);
        worldGroup.getChildren().add(workplane);

        // 2. Luz Ambiental
        javafx.scene.AmbientLight ambientLight = new javafx.scene.AmbientLight(Color.rgb(40, 40, 40));
        root3D.getChildren().add(ambientLight);

        // 3. Luz Principal
        javafx.scene.PointLight pointLight = new javafx.scene.PointLight(Color.WHITE);
        pointLight.setTranslateX(200);
        pointLight.setTranslateY(-500);
        pointLight.setTranslateZ(-200);
        pointLight.setConstantAttenuation(1.0);
        pointLight.setMaxRange(2000);
        root3D.getChildren().add(pointLight);
    }

    private static Group createWorkplane(double size) {
        Group planeGroup = new Group();

        // Base sólida
        double floorThickness = 1.0;
        Box base = new Box(size, floorThickness, size);
        PhongMaterial baseMat = new PhongMaterial(Color.web("#2b2b2b"));
        baseMat.setSpecularColor(Color.rgb(10, 10, 10));
        base.setMaterial(baseMat);
        base.setTranslateY(floorThickness / 2.0);

        // Rejilla
        PhongMaterial gridMat = new PhongMaterial(Color.web("#555555"));
        double lineThickness = 0.3;

        for (double i = -size / 2; i <= size / 2; i += 50) {
            Box lineX = new Box(lineThickness, lineThickness, size);
            lineX.setMaterial(gridMat);
            lineX.setTranslateX(i);
            lineX.setTranslateY(-0.02);

            Box lineZ = new Box(size, lineThickness, lineThickness);
            lineZ.setMaterial(gridMat);
            lineZ.setTranslateZ(i);
            lineZ.setTranslateY(-0.02);

            planeGroup.getChildren().addAll(lineX, lineZ);
        }
        planeGroup.getChildren().add(base);
        return planeGroup;
    }
}