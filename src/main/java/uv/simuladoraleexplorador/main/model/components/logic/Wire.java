package uv.simuladoraleexplorador.main.model.components.logic;

import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;

public class Wire {
    // Origen (El Arduino)
    private ElectronicComponent sourceComponent;
    private int sourcePin; // Ej: 13

    // Destino (El Motor o LED)
    private ElectronicComponent targetComponent;
    private String targetInputName; // Ej: "PWM_Input" o "Voltage"

    public Wire(ElectronicComponent source, int pin, ElectronicComponent target, String inputName) {
        this.sourceComponent = source;
        this.sourcePin = pin;
        this.targetComponent = target;
        this.targetInputName = inputName;
    }

    public ElectronicComponent getSourceComponent() { return sourceComponent; }
    public int getSourcePin() { return sourcePin; }
    public ElectronicComponent getTargetComponent() { return targetComponent; }
    public String getTargetInputName() { return targetInputName; }

    @Override
    public String toString() {
        return "Pin " + sourcePin + " ➔ " + targetComponent.getName() + " (" + targetInputName + ")";
    }
}