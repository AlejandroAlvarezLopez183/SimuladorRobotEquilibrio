package uv.simuladoraleexplorador.main.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DataManager {

    // Carpetas donde guardaremos todo
    private static final String DATA_FOLDER = "SimulatorData";
    private static final String MODELS_FOLDER = DATA_FOLDER + "/models";
    private static final String DB_FILE = DATA_FOLDER + "/components_db.json";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 1. Inicializar carpetas al arrancar (Llamar esto en el Main es buena práctica)
    public static void initStorage() {
        try {
            // Esto crea SimulatorData Y SimulatorData/models de un golpe
            Files.createDirectories(Paths.get(MODELS_FOLDER));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 2. Guardar un nuevo componente
    public static void saveComponent(ElectronicComponent component, String originalObjPath) {
        // Aseguramos que las carpetas existan ANTES de intentar copiar nada
        initStorage();

        // A. Copiar el .obj a nuestra carpeta interna
        if (originalObjPath != null && !originalObjPath.isEmpty()) {
            try {
                Path source = Paths.get(originalObjPath);

                // Verificamos que el archivo origen realmente exista para no crashear
                if (Files.exists(source)) {
                    String fileName = source.getFileName().toString();
                    String uniqueName = System.currentTimeMillis() + "_" + fileName;
                    Path dest = Paths.get(MODELS_FOLDER, uniqueName);

                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                    component.setModelPath(dest.toString());
                }
            } catch (IOException e) {
                System.err.println("Error copiando modelo 3D: " + e.getMessage());
            }
        }

        // B. Guardar en JSON
        List<ElectronicComponent> list = loadAllComponents();
        list.add(component);
        saveListToDisk(list);
    }

    public static List<ElectronicComponent> loadAllComponents() {
        File file = new File(DB_FILE);
        if (!file.exists()) return new ArrayList<>();

        try (Reader reader = new FileReader(file)) {
            List<ElectronicComponent> list = gson.fromJson(reader, new TypeToken<List<ElectronicComponent>>(){}.getType());
            return list != null ? list : new ArrayList<>(); // Evitar null si el archivo está vacío
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static void saveListToDisk(List<ElectronicComponent> list) {
        // --- AQUÍ ESTABA EL ERROR ---
        // Antes de escribir, verificamos que la carpeta padre exista.
        File file = new File(DB_FILE);
        File parentDir = file.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // ¡Créala si no existe!
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(list, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteComponent(ElectronicComponent componentToDelete) {
        // 1. Cargar la lista actual
        List<ElectronicComponent> list = loadAllComponents();

        // 2. Filtrar y remover el componente
        // Comparamos por nombre y tipo para estar seguros (idealmente usaríamos IDs únicos, pero esto sirve)
        boolean removed = list.removeIf(c ->
                c.getName().equals(componentToDelete.getName()) &&
                        c.getType() == componentToDelete.getType()
        );

        // 3. Si algo se borró, guardamos la lista actualizada
        if (removed) {
            saveListToDisk(list);

            // Opcional: Aquí podrías intentar borrar el archivo .obj si quieres ahorrar espacio,
            // pero es arriesgado si otros componentes usan el mismo modelo.
            // Por seguridad, hoy solo borramos el registro de la BD.
        }
    }
}