package uv.simuladoraleexplorador.main.ui.view3d;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import uv.simuladoraleexplorador.main.model.components.ComponentType;
import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;
import uv.simuladoraleexplorador.main.model.components.logic.Wire;
import uv.simuladoraleexplorador.main.model.components.logic.WiringManager;

public class WiringHandler {

    private final World3D world3D;
    private boolean isWiringMode = false;

    // Propiedad para actualizar el texto de abajo
    private final StringProperty statusMessage = new SimpleStringProperty("Modo Selección");

    // Datos temporales de la conexión en progreso
    private ElectronicComponent sourceComponent = null;
    private int sourcePin = -1;

    public WiringHandler(World3D world3D) {
        this.world3D = world3D;
        setupClickDetection();
    }

    // Activamos el modo "Tengo un cable en la mano"
    public void startWiringMode() {
        isWiringMode = true;
        sourceComponent = null;
        statusMessage.set("⚡ MODO CABLE: Selecciona el Componente ORIGEN");
    }

    public void cancelWiring() {
        isWiringMode = false;
        sourceComponent = null;
        statusMessage.set("Modo Selección");
    }

    private void setupClickDetection() {
        // Escuchamos clics en la escena 3D
        world3D.getSubScene().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (!isWiringMode || event.getButton() != MouseButton.PRIMARY) return;

            // 1. Detectar qué objeto 3D tocamos (Raycasting)
            javafx.scene.Node pickedNode = event.getPickResult().getIntersectedNode();

            // Buscamos hacia arriba en la jerarquía si tocamos una parte pequeña del modelo
            ElectronicComponent comp = findComponentInNode(pickedNode);

            if (comp != null) {
                // ¡BINGO! Tocamos un componente. Mostramos el menú estilo Packet Tracer
                showPortMenu(comp, event.getScreenX(), event.getScreenY());
            }
        });
    }

    private ElectronicComponent findComponentInNode(javafx.scene.Node node) {
        // Subimos por los padres hasta encontrar uno que tenga UserData
        javafx.scene.Node current = node;
        while (current != null) {
            if (current.getUserData() instanceof ElectronicComponent) {
                return (ElectronicComponent) current.getUserData();
            }
            current = current.getParent();
        }
        return null;
    }

    // --- AQUÍ ESTÁ LA MAGIA VISUAL ---
    private void showPortMenu(ElectronicComponent comp, double screenX, double screenY) {
        ContextMenu popup = new ContextMenu();
        popup.setStyle("-fx-background-color: #333; -fx-text-fill: white;");

        // Título del Menú (El nombre del componente)
        MenuItem title = new MenuItem(comp.getName() + " (" + comp.getType().getLabel() + ")");
        title.setDisable(true);
        title.setStyle("-fx-font-weight: bold; -fx-opacity: 1.0;");
        popup.getItems().add(title);
        popup.getItems().add(new SeparatorMenuItem());

        // Llenar puertos según el tipo
        if (comp.getType() == ComponentType.MICROCONTROLLER) {
            // == ARDUINO: Mostrar pines digitales ==
            for (int i = 0; i <= 13; i++) {
                final int pinNum = i;
                MenuItem item = new MenuItem("Digital Pin " + i + " (D" + i + ")");
                item.setOnAction(e -> handlePortSelection(comp, pinNum, "D" + pinNum));
                popup.getItems().add(item);
            }
            // (Aquí podrías agregar los analógicos A0-A5 si quieres)

        } else {
            // == MOTORES / SENSORES / LEDS ==
            // Usamos las propiedades que definimos en el Enum como "Puertos"
            // O definimos puertos estándar
            if (comp.getType() == ComponentType.DC_MOTOR) {
                addPortItem(popup, comp, "PWM Input");
                addPortItem(popup, comp, "Enable Pin");
            } else if (comp.getType() == ComponentType.LED) {
                addPortItem(popup, comp, "Anode (+)");
            } else {
                addPortItem(popup, comp, "Main Input");
            }
        }

        // Mostrar el menú justo donde está el mouse
        popup.show(world3D.getSubScene(), screenX, screenY);
    }

    private void addPortItem(ContextMenu menu, ElectronicComponent comp, String portName) {
        MenuItem item = new MenuItem("Puerto: " + portName);
        item.setOnAction(e -> handlePortSelection(comp, -1, portName)); // -1 porque no es un pin numerado
        menu.getItems().add(item);
    }

    // --- LÓGICA DE CONEXIÓN (MAQUINA DE ESTADOS) ---
    private void handlePortSelection(ElectronicComponent comp, int pinIndex, String portName) {

        if (sourceComponent == null) {
            // PASO 1: Es el primer clic (Origen)
            if (comp.getType() != ComponentType.MICROCONTROLLER) {
                statusMessage.set("⚠️ Error: El origen debe ser un Microcontrolador por ahora.");
                return;
            }
            sourceComponent = comp;
            sourcePin = pinIndex;
            statusMessage.set("🔗 Origen: " + comp.getName() + " [Pin " + pinIndex + "] ➔ Selecciona DESTINO...");

        } else {
            // PASO 2: Es el segundo clic (Destino)
            if (comp == sourceComponent) {
                statusMessage.set("⚠️ Error: No puedes conectar el componente a sí mismo.");
                return;
            }

            // ¡CREAR CONEXIÓN!
            Wire newWire = new Wire(sourceComponent, sourcePin, comp, portName);
            WiringManager.getInstance().addConnection(newWire);

            statusMessage.set("✅ Conectado: " + sourceComponent.getName() + " ➔ " + comp.getName());

            // Reiniciamos para el siguiente cable (manteniendo el modo cable activo)
            sourceComponent = null;
        }
    }

    public StringProperty statusMessageProperty() { return statusMessage; }
}