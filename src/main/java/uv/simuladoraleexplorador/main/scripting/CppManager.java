package uv.simuladoraleexplorador.main.scripting;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.*;
import java.nio.file.Files;
import java.util.function.Consumer;

public class CppManager {

    private Context context;
    private Value bindings; // Acceso a las funciones del C++ cargado

    // Inicializamos el entorno LLVM
    public CppManager() {
        this.context = Context.newBuilder("llvm")
                .allowAllAccess(true)
                .build();
    }

    // 1. Fase de Compilación (Texto -> Archivo .bc)
    public File compile(String cppCode, Consumer<String> logger) {
        try {
            // A. Crear archivo temporal .cpp
            File sourceFile = File.createTempFile("arduino_sketch", ".cpp");
            Files.writeString(sourceFile.toPath(), cppCode);

            // B. Preparar archivo de salida .bc (Bitcode)
            File outputFile = File.createTempFile("arduino_sketch", ".bc");

            // C. Invocar a CLANG (El compilador del sistema)
            // Comando: clang -O1 -c -emit-llvm -o salida.bc entrada.cpp
            ProcessBuilder pb = new ProcessBuilder(
                    "clang",
                    "-O1",
                    "-c",
                    "-emit-llvm",
                    "-target", "x86_64-unknown-linux-gnu", // <--- AGREGA ESTA LÍNEA MÁGICA
                    "-o", outputFile.getAbsolutePath(),
                    sourceFile.getAbsolutePath()
            );

            pb.redirectErrorStream(true); // Juntar errores con salida normal
            Process process = pb.start();

            // Leer lo que diga el compilador (Errores o warnings)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.accept("[CLANG]: " + line + "\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.accept(">> Compilación Exitosa.\n");
                return outputFile;
            } else {
                logger.accept(">> Error de Compilación (Código " + exitCode + ")\n");
                return null;
            }

        } catch (Exception e) {
            logger.accept("Error Crítico: " + e.getMessage());
            return null;
        }
    }

    public Value loadBindings(File bitcodeFile, Consumer<String> logger) {
        try {
            Source source = Source.newBuilder("llvm", bitcodeFile).build();
            // Ejecutamos el archivo para cargar las funciones en memoria
            Value bindings = context.eval(source);

            logger.accept(">> Firmware cargado en memoria RAM.\n");
            return bindings; // Devolvemos el acceso a las funciones (setup, loop, etc)

        } catch (Exception e) {
            logger.accept("Error cargando binario: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        if (context != null) {
            try {
                // Forzamos el cierre del motor LLVM
                context.close(true);
            } catch (Exception e) {
                System.err.println("Error cerrando C++: " + e.getMessage());
            }
        }
    }
}