package uv.simuladoraleexplorador.main.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;
import uv.simuladoraleexplorador.main.model.components.logic.Wire;
import uv.simuladoraleexplorador.main.model.components.logic.WiringManager;
import uv.simuladoraleexplorador.main.model.components.save.SavedObject;
import uv.simuladoraleexplorador.main.model.components.save.SavedWire;
import uv.simuladoraleexplorador.main.model.components.save.SimulationState;
import uv.simuladoraleexplorador.main.ui.view3d.RobotManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ProjectManager {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // --- GUARDAR ---
    public static void saveProject(Stage stage, RobotManager robotManager, String currentCode) {
        SimulationState state = new SimulationState();
        state.timestamp = System.currentTimeMillis();
        state.cppCode = currentCode;

        // 1. GUARDAR OBJETOS
        state.objects = new ArrayList<>();
        for (ElectronicComponent comp : robotManager.getAllComponents()) {
            SavedObject obj = new SavedObject();
            obj.id = comp.getName();
            obj.type = comp.getType().name();
            obj.modelPath = comp.getModelPath();

            Node node = robotManager.getNodeFromComponent(comp);
            if (node != null) {
                javafx.geometry.Point3D worldPos = node.localToScene(0, 0, 0);

                obj.posX = worldPos.getX();
                obj.posY = worldPos.getY();
                obj.posZ = worldPos.getZ();

                // Guardar rotación y escala...
                obj.mass = (float) comp.getMass();
            }
            state.objects.add(obj);
        }

        // 2. GUARDAR CABLES
        state.wires = new ArrayList<>();
        for (Wire w : WiringManager.getInstance().getConnections()) {
            SavedWire sw = new SavedWire();
            sw.sourceName = w.getSourceComponent().getName();
            sw.sourcePin = w.getSourcePin();
            sw.targetName = w.getTargetComponent().getName();
            sw.targetInput = w.getTargetInputName();
            state.wires.add(sw);
        }

        // 3. ESCRIBIR EN DISCO
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Proyecto UV Sim");

        // MEJORA VISUAL: Ponemos la extensión en la descripción también
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Simulación UV (.uvsim)", "*.uvsim"));

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            // --- EL PARCHE PARA LINUX/DEBIAN ---
            // Si el usuario no escribió ".uvsim", se lo ponemos nosotros a la fuerza
            if (!file.getName().toLowerCase().endsWith(".uvsim")) {
                file = new File(file.getAbsolutePath() + ".uvsim");
            }
            // -----------------------------------

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(state, writer);
                System.out.println("✅ Proyecto guardado en: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- CARGAR ---
    public static SimulationState loadProject(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Abrir Proyecto UV Sim");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos UV Sim", "*.uvsim"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, SimulationState.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null; // Cancelado o Error
    }
}