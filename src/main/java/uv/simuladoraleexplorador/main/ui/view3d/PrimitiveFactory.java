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
        TriangleMesh mesh = new TriangleMesh();

        // Usamos la mitad del tamaño para centrarlo en el origen
        float hs = (float) size / 2.0f;

        // Vértices Centrados: La base está en Y = hs (abajo), la punta en Y = -hs (arriba)
        mesh.getPoints().addAll(
                // Base (4 esquinas en Y positivo)
                -hs, hs, -hs,   // 0: Front-Left-Bottom
                hs, hs, -hs,   // 1: Front-Right-Bottom
                -hs, hs,  hs,   // 2: Back-Left-Bottom
                hs, hs,  hs,   // 3: Back-Right-Bottom

                // Top (Los 2 puntos altos atrás en Y negativo)
                -hs, -hs, hs,   // 4: Back-Left-Top
                hs, -hs, hs    // 5: Back-Right-Top
        );

        mesh.getTexCoords().addAll(0, 0);

        // Caras (Re-conectando los puntos nuevos)
        mesh.getFaces().addAll(
                // Base
                0,0, 1,0, 3,0,
                0,0, 3,0, 2,0,
                // Frente (Rectángulo bajo)
                0,0, 2,0, 1,0, // Invertido para que se vea por fuera
                // Atrás (Rectángulo alto)
                2,0, 3,0, 5,0,
                2,0, 5,0, 4,0,
                // Rampa (La pendiente)
                0,0, 1,0, 5,0,
                0,0, 5,0, 4,0,
                // Lados (Triángulos)
                0,0, 4,0, 2,0, // Izquierda
                1,0, 3,0, 5,0  // Derecha
        );

        MeshView view = new MeshView(mesh);
        view.setMaterial(DEFAULT_MAT);

        return new Group(view);
    }
}