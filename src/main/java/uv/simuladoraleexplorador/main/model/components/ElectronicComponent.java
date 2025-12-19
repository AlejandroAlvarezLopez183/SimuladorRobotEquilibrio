package uv.simuladoraleexplorador.main.model.components;

import java.util.HashMap;
import java.util.Map;

public class ElectronicComponent {
    private String name;
    private ComponentType type;

    // Aquí guardamos los datos variables:
    // "Voltaje" -> 5.0, "RPM" -> 200.0, etc.
    private Map<String, String> specs = new HashMap<>();

    public ElectronicComponent(String name, ComponentType type) {
        this.name = name;
        this.type = type;
    }

    public void setSpec(String key, String value) {
        specs.put(key, value);
    }

    public String getSpec(String key) {
        return specs.getOrDefault(key, "0");
    }

    // Getters básicos
    public String getName() { return name; }
    public ComponentType getType() { return type; }
    public Map<String, String> getSpecs() { return specs; }

    @Override
    public String toString() {
        return name + " (" + type.getLabel() + ")";
    }
}