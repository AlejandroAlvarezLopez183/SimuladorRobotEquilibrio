package uv.simuladoraleexplorador.main.model.components;

public enum ComponentType {
    // Definimos el Tipo y la lista de "Etiquetas" que pediremos en el formulario
    DC_MOTOR("Motor DC", new String[]{"Voltaje (V)", "RPM Máx", "Torque (kg/cm)"}),
    SERVO_MOTOR("Servomotor", new String[]{"Voltaje (V)", "Rango (Grados)", "Velocidad (sec/60°)"}),
    ULTRASONIC_SENSOR("Sensor Ultrasónico", new String[]{"Voltaje (V)", "Rango Máx (cm)", "Precisión (cm)"}),
    RASPBERRY_PI("Microcontrolador", new String[]{"Voltaje (V)", "Pines Digitales", "RAM (MB)"}),
    LED("Luz LED", new String[]{"Voltaje (V)", "Color (Hex)", "Consumo (mA)"});

    private final String label;
    private final String[] properties;

    ComponentType(String label, String[] properties) {
        this.label = label;
        this.properties = properties;
    }

    public String getLabel() { return label; }
    public String[] getProperties() { return properties; }

    @Override
    public String toString() { return label; }
}