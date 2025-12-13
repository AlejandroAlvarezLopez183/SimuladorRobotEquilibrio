package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;

public class PrimitiveFactory {

    private static final PhongMaterial DEFAULT_MAT = new PhongMaterial(Color.web("#ffcc80")); // Naranja prueba

    public static Group createCube(double size) {
        Box box = new Box(size, size, size);
        box.setMaterial(DEFAULT_MAT);
        return new Group(box);
    }

    public static Group createSphere(double radius) {
        Sphere sphere = new Sphere(radius);
        sphere.setMaterial(DEFAULT_MAT);
        return new Group(sphere);
    }

    public static Group createCylinder(double radius, double height) {
        Cylinder cylinder = new Cylinder(radius, height);
        cylinder.setMaterial(DEFAULT_MAT);
        return new Group(cylinder);
    }

    public static Group createRamp(double size) {
        // Crear una cuña (Wedge) usando TriangleMesh
        TriangleMesh mesh = new TriangleMesh();

        // Vértices de una rampa simple
        float s = (float) size;
        mesh.getPoints().addAll(
                0, 0, 0,    // 0: Front-Bottom-Left
                s, 0, 0,    // 1: Front-Bottom-Right
                0, -s, 0,   // 2: Front-Top-Left (La altura)
                0, 0, s,    // 3: Back-Bottom-Left
                s, 0, s,    // 4: Back-Bottom-Right
                0, -s, s    // 5: Back-Top-Left
        );

        // Coordenadas de textura (Dummy)
        mesh.getTexCoords().addAll(0, 0);

        // Caras (Faces)
        mesh.getFaces().addAll(
                0,0, 1,0, 2,0,  // Frente
                3,0, 5,0, 4,0,  // Atrás
                0,0, 3,0, 4,0,  // Abajo 1
                0,0, 4,0, 1,0,  // Abajo 2
                2,0, 1,0, 4,0,  // Rampa (Diagonal) 1
                2,0, 4,0, 5,0,  // Rampa (Diagonal) 2
                2,0, 5,0, 3,0,  // Lado izquierdo 1
                2,0, 3,0, 0,0   // Lado izquierdo 2
        );

        MeshView view = new MeshView(mesh);
        view.setMaterial(DEFAULT_MAT);

        // Ajuste para centrar visualmente aprox
        view.setTranslateX(-s/2);
        view.setTranslateY(s/2);
        view.setTranslateZ(-s/2);

        return new Group(view);
    }
}