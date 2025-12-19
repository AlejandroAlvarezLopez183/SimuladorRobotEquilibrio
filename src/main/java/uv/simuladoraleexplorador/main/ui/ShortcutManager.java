package uv.simuladoraleexplorador.main.ui;

import javafx.scene.layout.Pane;
import javafx.scene.input.KeyEvent;

public class ShortcutManager {

    private final MainController controller;

    public ShortcutManager(MainController controller) {
        this.controller = controller;
    }

    public void init(Pane targetPane) {
        targetPane.setFocusTraversable(true);
        targetPane.setOnKeyPressed(this::handleKey);
        targetPane.setOnMousePressed(e -> targetPane.requestFocus());
    }

    private void handleKey(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case G:
                    controller.handleGroupObjects();
                    break;
                case U:
                    controller.handleUngroupObjects();
                    break;
            }
        } else {
            switch (event.getCode()) {
                case DELETE:
                    controller.handleDeleteSelected();
                    break;
            }
        }
    }
}