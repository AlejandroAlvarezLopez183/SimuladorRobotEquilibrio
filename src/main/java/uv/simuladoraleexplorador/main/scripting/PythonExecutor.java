package uv.simuladoraleexplorador.main.scripting;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class PythonExecutor {

    private Context context;
    private final ByteArrayOutputStream outputStream;

    public PythonExecutor() {
        this.outputStream = new ByteArrayOutputStream();
        initContext();
    }

    private void initContext() {
        // Configuramos el motor.
        // "python" es el lenguaje.
        // option("python.ForceImportSite", "true") ayuda a que no falle en Windows/Linux sin Python instalado.
        this.context = Context.newBuilder("python")
                .allowAllAccess(true)
                .option("python.ForceImportSite", "true")
                .out(outputStream)
                .err(outputStream)
                .build();
    }

    public void runScript(String code, Consumer<String> outputCallback) {
        // Ejecutamos en otro hilo para que no se congele tu simulador
        new Thread(() -> {
            try {
                outputStream.reset(); // Limpiar consola anterior

                context.eval("python", code); // <--- AQUÍ OCURRE LA MAGIA

                String result = outputStream.toString(StandardCharsets.UTF_8);
                outputCallback.accept(result);

            } catch (PolyglotException e) {
                outputCallback.accept("Error de Python: " + e.getMessage());
            } catch (Exception e) {
                outputCallback.accept("Error Java: " + e.getMessage());
            }
        }).start();
    }

    public void close() {
        if (context != null) context.close();
    }
}