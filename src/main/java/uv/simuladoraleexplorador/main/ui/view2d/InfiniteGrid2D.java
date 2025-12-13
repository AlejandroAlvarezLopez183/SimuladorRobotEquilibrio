package uv.simuladoraleexplorador.main.ui.view2d;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class InfiniteGrid2D extends Canvas {

    // Colores estilo Godot/Blender
    private static final Color BACKGROUND = Color.web("#181818");
    private static final Color LINE_MINOR = Color.web("#353535"); // Gris tenue
    private static final Color LINE_MAJOR = Color.web("#111111"); // Negro suave para divisiones grandes
    private static final Color AXIS_X = Color.web("#ff5f5f"); // Rojo
    private static final Color AXIS_Y = Color.web("#5fdf5f"); // Verde

    public InfiniteGrid2D(double width, double height) {
        super(width, height);
        // Redibujar cuando cambie el tamaño
        widthProperty().addListener(evt -> draw());
        heightProperty().addListener(evt -> draw());
    }

    public void draw() {
        double w = getWidth();
        double h = getHeight();
        GraphicsContext gc = getGraphicsContext2D();

        // 1. Limpiar fondo
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);

        // Centro de la pantalla (el origen 0,0)
        double centerX = w / 2;
        double centerY = h / 2;

        // Tamaño de celda
        double gridSize = 20.0;

        // 2. Dibujar líneas menores (la rejilla fina)
        gc.setStroke(LINE_MINOR);
        gc.setLineWidth(1.0);
        gc.beginPath();

        // Líneas verticales
        for (double x = centerX % gridSize; x < w; x += gridSize) {
            gc.moveTo(x, 0);
            gc.lineTo(x, h);
        }
        // Líneas horizontales
        for (double y = centerY % gridSize; y < h; y += gridSize) {
            gc.moveTo(0, y);
            gc.lineTo(w, y);
        }
        gc.stroke();

        // 3. Dibujar Ejes Principales (X e Y tipo Godot)
        gc.setLineWidth(2.0);

        // Eje Y (Verde)
        gc.setStroke(AXIS_Y);
        gc.strokeLine(centerX, 0, centerX, h);

        // Eje X (Rojo)
        gc.setStroke(AXIS_X);
        gc.strokeLine(0, centerY, w, centerY);
    }

    // Método público para forzar repintado si es necesario
    public void refresh() {
        draw();
    }
}