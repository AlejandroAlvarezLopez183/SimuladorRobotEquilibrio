package uv.simuladoraleexplorador.main.model.components.logic;

import uv.simuladoraleexplorador.main.model.components.ElectronicComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WiringManager {

    private static WiringManager instance;
    private List<Wire> connections = new ArrayList<>();

    private WiringManager() {}

    public static WiringManager getInstance() {
        if (instance == null) instance = new WiringManager();
        return instance;
    }

    // Crear una conexión
    public void addConnection(Wire wire) {
        // Opcional: Verificar que no exista ya algo conectado a ese pin
        connections.add(wire);
        System.out.println("🔗 Nueva conexión: " + wire);
    }

    // Borrar conexión
    public void removeConnection(Wire wire) {
        connections.remove(wire);
    }

    public List<Wire> getConnections() {
        return connections;
    }

    // Esto lo llamará tu GenericMicrocontroller cuando C++ haga digitalWrite
    public void transmitSignal(ElectronicComponent source, int pin, int value) {
        // Buscamos todos los cables conectados a este pin de este componente
        List<Wire> cables = connections.stream()
                .filter(w -> w.getSourceComponent() == source && w.getSourcePin() == pin)
                .collect(Collectors.toList());

        for (Wire wire : cables) {
            // Le avisamos al componente destino que recibió señal
            ElectronicComponent target = wire.getTargetComponent();
            target.receiveSignal(wire.getTargetInputName(), value);
        }
    }
}