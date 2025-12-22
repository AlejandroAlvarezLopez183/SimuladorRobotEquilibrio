package uv.simuladoraleexplorador.main.model.components.data;

import java.util.HashMap;
import java.util.Map;

// Esta clase NO tiene lógica, solo DATOS.
// Es lo que convertiremos a JSON.
public class MicrocontrollerData {

    // 1. Identidad Visual
    public String id;           // Ej: "arduino_uno_r3"
    public String name;         // Ej: "Arduino Uno Rev3"
    public String modelPath;    // Ej: "assets/models/boards/uno.obj"
    public String iconPath;     // Ej: "assets/icons/uno_icon.png"

    // 2. Especificaciones Eléctricas (Lo que pediste)
    public double operatingVoltage; // Ej: 5.0 (V)
    public double maxInputVoltage;  // Ej: 12.0 (V)
    public double clockSpeed;       // Ej: 16 (MHz) - Para calcular velocidad de simulación

    // 3. Capacidad de Pines
    public int totalDigitalPins;    // Ej: 14
    public int totalAnalogPins;     // Ej: 6
    public boolean hasPWM;          // true

    // 4. Mapeo de Pines (Clave: Pin Físico, Valor: Tipo)
    // Ej: Pin 0 -> "RX", Pin 1 -> "TX", Pin 13 -> "LED_BUILTIN"
    public Map<Integer, String> specialPins = new HashMap<>();

    // Constructor vacío necesario para cargar JSONs
    public MicrocontrollerData() {}

    // Constructor rápido para pruebas
    public MicrocontrollerData(String name, String modelPath, int digPins) {
        this.name = name;
        this.modelPath = modelPath;
        this.totalDigitalPins = digPins;
    }
}