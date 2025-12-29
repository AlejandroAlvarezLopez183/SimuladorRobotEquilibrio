package uv.simuladoraleexplorador.main.ui.view3d.visuals;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class VisualWire {

    private final Cylinder cord;
    private final Node startNode; // El Arduino
    private final Node endNode;   // El Motor

    public VisualWire(Node start, Node end) {
        this.startNode = start;
        this.endNode = end;

        // 1. Crear el aspecto del cable
        // Radio 0.5 (delgado), Altura 1 (se cambiará dinámicamente)
        this.cord = new Cylinder(0.8, 1);

        // Material: Naranja estilo Packet Tracer
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.ORANGE);
        mat.setSpecularColor(Color.ORANGERED);
        this.cord.setMaterial(mat);
        update();
    }

    public Cylinder getShape() {
        return cord;
    }

    // Este método se llamará 60 veces por segundo
    public void update() {
        if (startNode == null || endNode == null) return;

        // 1. Obtener posiciones globales en el mundo
        Point3D p1 = startNode.localToScene(0, 0, 0);
        Point3D p2 = endNode.localToScene(0, 0, 0);

        // 2. Calcular el vector diferencia (La flecha de A a B)
        Point3D diff = p2.subtract(p1);
        double length = diff.magnitude();

        // 3. Calcular Punto Medio (Aquí va el centro del cilindro)
        Point3D mid = p1.midpoint(p2);

        // 4. Aplicar Transformaciones
        cord.getTransforms().clear();

        // A. Mover al punto medio
        cord.getTransforms().add(new Translate(mid.getX(), mid.getY(), mid.getZ()));

        // B. Rotar para apuntar al destino
        // El eje Y es el "arriba" natural del cilindro. Calculamos cuánto rotarlo.
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axisOfRotation = yAxis.crossProduct(diff);
        double angle = yAxis.angle(diff);

        cord.getTransforms().add(new Rotate(angle, axisOfRotation));

        // C. Ajustar largo (Altura del cilindro)
        cord.setHeight(length);
    }
}