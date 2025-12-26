package uv.simuladoraleexplorador.main.model.components;

public enum ComponentType {
    DC_MOTOR("Motor DC", new String[]{"Voltaje (V)", "RPM Máx", "Torque (kg/cm)"}),
    SERVO_MOTOR("Servomotor", new String[]{"Voltaje (V)", "Rango (Grados)", "Velocidad (sec/60°)"}),
    ULTRASONIC_SENSOR("Sensor Ultrasónico", new String[]{"Voltaje (V)", "Rango Máx (cm)", "Precisión (cm)"}),
    RASPBERRY_PI("Raspberry Pi", new String[]{"Voltaje (V)", "Pines Digitales", "RAM (MB)"}),
    LED("Luz LED", new String[]{"Voltaje (V)", "Color (Hex)", "Consumo (mA)"}),

    // --- ESTA ES LA LÍNEA QUE TE FALTA O TIENE ERROR ---
    MICROCONTROLLER("Microcontrolador", new String[]{"Voltaje Op (V)", "Pines Digitales", "Pines Analogicos"});
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