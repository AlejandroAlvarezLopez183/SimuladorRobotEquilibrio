package uv.simuladoraleexplorador.main;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uv.simuladoraleexplorador.main.ui.SplashController;
import uv.simuladoraleexplorador.main.ui.MainController; // Importamos tu nueva clase maestra

import java.io.IOException;

public class App extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        mostrarSplashYCargar();
    }

    private void mostrarSplashYCargar() throws IOException {
        // 1. Cargar Splash
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/splash_screen.fxml"));
        Parent root = loader.load();
        SplashController splashCtrl = loader.getController();

        // Configurar ventana de carga sin bordes
        Stage splashStage = new Stage();
        splashStage.setScene(new Scene(root));
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.show();

        // 2. Tarea de Carga (Simulada o Real)
        Task<Void> loadTask = createLoadingTask();

        // Vincular barra de progreso
        splashCtrl.updateProgress(0, "Iniciando...");
        loadTask.messageProperty().addListener((obs, old, msg) -> splashCtrl.updateProgress(loadTask.getProgress(), msg));
        loadTask.progressProperty().addListener((obs, old, val) -> splashCtrl.updateProgress(val.doubleValue(), loadTask.getMessage()));

        // 3. TRANSICIÓN: Cuando termine, cerramos Splash y abrimos Main
        loadTask.setOnSucceeded(e -> {
            splashStage.close();
            lanzarMenuPrincipal(); // <--- Aquí pasamos el control
        });

        new Thread(loadTask).start();
    }

    // Este método inicia la clase maestra que pediste
    private void lanzarMenuPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_layout.fxml"));
            Parent root = loader.load();

            // Obtener TU clase maestra
            MainController controller = loader.getController();

            // Crear la escena principal
            Scene scene = new Scene(root);

            // Pasar la configuración final al Stage principal y mostrarl
            primaryStage.setScene(scene);

            // Le decimos al controller: "Toma el control del Stage"
            controller.iniciarSistema(primaryStage);

            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error fatal al cargar el menú principal.");
        }
    }

    private Task<Void> createLoadingTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Cargando módulos...");
                updateProgress(0.2, 1.0);
                Thread.sleep(800);

                updateMessage("Configurando entorno 3D...");
                updateProgress(0.6, 1.0);
                Thread.sleep(800);

                updateMessage("Listo.");
                updateProgress(1.0, 1.0);
                Thread.sleep(400);
                return null;
            }
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}