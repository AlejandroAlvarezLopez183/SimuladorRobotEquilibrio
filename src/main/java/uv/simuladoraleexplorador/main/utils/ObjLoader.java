package uv.simuladoraleexplorador.main.utils;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ObjLoader {

    public static Group loadModel(File file) {
        Group modelRoot = new Group();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            TriangleMesh mesh = new TriangleMesh();
            List<Float> vertices = new ArrayList<>();
            List<Float> texCoords = new ArrayList<>();
            List<Integer> faces = new ArrayList<>();

            // Texturas dummy (necesarias para que JavaFX no se queje)
            texCoords.add(0f); texCoords.add(0f);

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;

                String[] parts = line.split("\\s+");

                switch (parts[0]) {
                    case "v": // Vértices (x, y, z)
                        vertices.add(Float.parseFloat(parts[1]));
                        vertices.add(Float.parseFloat(parts[2]));
                        vertices.add(Float.parseFloat(parts[3]));
                        break;
                    case "f": // Caras (indices de vértices)
                        // Soporta caras de 3 (triángulos) o 4 (quads) vértices
                        processFace(parts, faces, vertices.size() / 3);
                        break;
                }
            }

            // Convertir listas a arrays nativos de JavaFX
            float[] floatVertices = new float[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) floatVertices[i] = vertices.get(i);

            float[] floatTex = new float[texCoords.size()];
            for (int i = 0; i < texCoords.size(); i++) floatTex[i] = texCoords.get(i);

            int[] intFaces = new int[faces.size()];
            for (int i = 0; i < faces.size(); i++) intFaces[i] = faces.get(i);

            mesh.getPoints().addAll(floatVertices);
            mesh.getTexCoords().addAll(floatTex);
            mesh.getFaces().addAll(intFaces);

            // Crear el objeto visual
            MeshView meshView = new MeshView(mesh);

            // ESTILO: Material gris metálico por defecto
            PhongMaterial material = new PhongMaterial(Color.SILVER);
            material.setSpecularColor(Color.WHITE);
            meshView.setMaterial(material);
            meshView.setCullFace(CullFace.NONE); // Dibujar ambos lados de las caras
            meshView.setDrawMode(javafx.scene.shape.DrawMode.FILL);

            modelRoot.getChildren().add(meshView);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error cargando modelo: " + file.getName());
        }

        return modelRoot;
    }

    private static void processFace(String[] parts, List<Integer> faces, int maxVerts) {
        // Lógica simple para convertir polígonos a triángulos
        // Formato OBJ: f v1/vt1/vn1 v2/vt2/vn2 ...

        int[] vIndex = new int[parts.length - 1];

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            // Tomamos solo el primer número (el índice del vértice)
            String vStr = part.split("/")[0];
            vIndex[i-1] = Integer.parseInt(vStr) - 1; // OBJ es base-1, Java es base-0
        }

        // Triangulación (Fan-style)
        // 0-1-2, 0-2-3, 0-3-4...
        for (int i = 0; i < vIndex.length - 2; i++) {
            faces.add(vIndex[0]); faces.add(0); // v0 + dummy texture
            faces.add(vIndex[i+1]); faces.add(0); // v1
            faces.add(vIndex[i+2]); faces.add(0); // v2
        }
    }
}