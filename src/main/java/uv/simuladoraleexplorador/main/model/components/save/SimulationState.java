package uv.simuladoraleexplorador.main.model.components.save;

import java.util.List;

public class SimulationState {
    public String version = "1.0";
    public long timestamp;
    public String cppCode;
    public List<SavedObject> objects;
    public List<SavedWire> wires;
}

