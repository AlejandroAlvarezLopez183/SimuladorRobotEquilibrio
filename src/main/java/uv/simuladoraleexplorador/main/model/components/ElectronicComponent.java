package uv.simuladoraleexplorador.main.model.components;

import java.util.HashMap;
import java.util.Map;

public class ElectronicComponent {
    private String name;
    private ComponentType type;
    private String modelPath;
    private double mass; // <--- NUEVO CAMPO (En Kg)
    private boolean isPowered = false;
    private int currentSignalLevel = 0;

    private Map<String, String> specs = new HashMap<>();

    public ElectronicComponent(String name, ComponentType type) {
        this.name = name;
        this.type = type;
        this.modelPath = "";
        this.mass = 0.5; // Valor por defecto (500 gramos)
    }

    // --- GETTERS Y SETTERS ---
    public void setMass(double mass) { this.mass = mass; }
    public double getMass() { return mass; }

    public void setModelPath(String path) { this.modelPath = path; }
    public String getModelPath() { return modelPath; }

    public void setSpec(String key, String value) { specs.put(key, value); }
    public String getSpec(String key) { return specs.getOrDefault(key, "0"); }

    public String getName() { return name; }
    public ComponentType getType() { return type; }
    public Map<String, String> getSpecs() { return specs; }

    @Override
    public String toString() { return name; }

    public void receiveSignal(String inputName, int value) {
        System.out.println("⚡ Componente " + this.getName() + " recibió " + value + " en puerto " + inputName);

        this.currentSignalLevel = value;

        // Ejemplo de lógica simple
        if (inputName.contains("ENABLE") || inputName.contains("ANODE")) {
            if (value > 0) {
                turnOn();
            } else {
                turnOff();
            }
        }
    }

    private void turnOn() {
        if (!isPowered) {
            isPowered = true;
            System.out.println("   🟢 " + getName() + " ENCENDIDO");
            // Aquí cambiarías el color del modelo 3D a brillante
        }
    }

    private void turnOff() {
        if (isPowered) {
            isPowered = false;
            System.out.println("   ⚪ " + getName() + " APAGADO");
            // Aquí volverías el color a normal
        }
    }
}