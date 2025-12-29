package uv.simuladoraleexplorador.main.logic.simulation;

import javafx.animation.AnimationTimer;
import uv.simuladoraleexplorador.main.model.physics.PhysicsEngine;

public class SimulationLoop extends AnimationTimer {

    private final PhysicsEngine physics;
    private boolean isPaused = true;
    private long lastTime = 0;

    public SimulationLoop(PhysicsEngine physics) {
        this.physics = physics;
    }

    @Override
    public void handle(long now) {
        if (lastTime == 0) { lastTime = now; return; }

        // Calcular Delta Time en segundos
        float timeStep = (now - lastTime) / 1_000_000_000.0f;
        lastTime = now;

        // Capar el delta time para evitar "explosiones" si la PC se congela
        if (timeStep > 0.1f) timeStep = 0.1f;

        if (!isPaused) {
            physics.stepSimulation(timeStep);
            checkKillFloor(); // Resetear objetos que caen al infinito
        }
    }

    public void play() { isPaused = false; lastTime = 0; start(); }
    public void pause() { isPaused = true; stop(); }

    private void checkKillFloor() {
        // Implementación simple: si Y < -2000, frenar o resetear
        // (Puedes copiar tu lógica de World3D aquí más tarde)
    }
}