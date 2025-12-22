package uv.simuladoraleexplorador.main.model.components.logic;

import uv.simuladoraleexplorador.main.model.components.data.MicrocontrollerData;

public class GenericMicrocontroller {

    private final MicrocontrollerData data; // Sus especificaciones
    private final int[] pinStates;          // Estado actual (0 o 1)

    public GenericMicrocontroller(MicrocontrollerData data) {
        this.data = data;
        this.pinStates = new int[data.totalDigitalPins];

        System.out.println(">> Inicializando: " + data.name);
        System.out.println("   Voltaje Operación: " + data.operatingVoltage + "V");
    }

    // Este es el método que llamará C++ (Bridge)
    public void digitalWrite(int pin, int value) {
        if (pin < 0 || pin >= pinStates.length) {
            System.err.println("ERROR: El " + data.name + " no tiene pin " + pin);
            return;
        }

        this.pinStates[pin] = value;

        // Verificar si es un pin especial (Ej: LED Integrado)
        if (data.specialPins.containsKey(pin)) {
            String function = data.specialPins.get(pin);
            if (function.equals("LED_BUILTIN")) {
                // Aquí llamamos a la función visual que hace brillar el modelo 3D
                System.out.println("💡 LED Integrado (" + data.name + ") -> " + (value==1 ? "ON" : "OFF"));
            }
        }
    }

    public int digitalRead(int pin) {
        if (pin < 0 || pin >= pinStates.length) return 0;
        return pinStates[pin];
    }

    public MicrocontrollerData getData() {
        return data;
    }
}