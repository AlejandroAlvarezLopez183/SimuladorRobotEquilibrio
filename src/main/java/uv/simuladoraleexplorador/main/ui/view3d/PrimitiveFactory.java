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

        float hs = (float) size / 2.0f;

        // 1. Vértices (Puntos en el espacio)
        mesh.getPoints().addAll(
                // Base (Y = hs)
                -hs,  hs, -hs,  // 0: Front-Left-Bottom
                hs,  hs, -hs,  // 1: Front-Right-Bottom
                -hs,  hs,  hs,  // 2: Back-Left-Bottom
                hs,  hs,  hs,  // 3: Back-Right-Bottom
                // Top atrás (Y = -hs)
                -hs, -hs,  hs,  // 4: Back-Left-Top
                hs, -hs,  hs   // 5: Back-Right-Top
        );

        // 2. Coordenadas de textura (Dummy)
        mesh.getTexCoords().addAll(0, 0);

        // 3. Caras (Índices de vértices)
        // Formato: punto1, textura1, punto2, textura2, punto3, textura3
        mesh.getFaces().addAll(
                // BASE (Abajo) - Mirando hacia abajo
                2,0, 3,0, 1,0,
                2,0, 1,0, 0,0,

                // ATRÁS (Rectángulo vertical) - Mirando hacia atrás
                3,0, 2,0, 4,0,
                3,0, 4,0, 5,0,

                // FRENTE (Rectángulo pequeño o línea base)
                // Como es una rampa que llega a 0 en el frente, solo unimos los puntos
                0,0, 1,0, 3,0, // Nota: Esto técnicamente es parte de los lados/base

                // LADO IZQUIERDO (Triángulo)
                0,0, 2,0, 4,0,

                // LADO DERECHO (Triángulo)
                1,0, 5,0, 3,0,

                // RAMPA / PENDIENTE (El plano inclinado)
                0,0, 4,0, 5,0,
                0,0, 5,0, 1,0
        );

        MeshView view = new MeshView(mesh);
        view.setMaterial(DEFAULT_MAT);

        // ESTO ES CLAVE: Para que no se vea transparente por dentro mientras pruebas
        view.setCullFace(javafx.scene.shape.CullFace.NONE);

        return new Group(view);
    }
}