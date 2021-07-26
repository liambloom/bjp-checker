package dev.liambloom.tests.bjp.shared;

import org.fusesource.jansi.Ansi;

public enum Color {
    RED("31", javafx.scene.paint.Color.RED),
    GREEN("32", javafx.scene.paint.Color.GREEN),
    YELLOW("33", javafx.scene.paint.Color.YELLOW),
    CYAN("36", javafx.scene.paint.Color.CYAN),
    GRAY("38;5;8", javafx.scene.paint.Color.GRAY),
    RESET("0", null);

    private final String ansi;
    private final javafx.scene.paint.Color jfx;

    Color(String ansi, javafx.scene.paint.Color jfx) {
        this.ansi = ansi;
        this.jfx = jfx;
    }

    public String ansi() {
        return ansi;
    }

    public javafx.scene.paint.Color jfx() {
        return jfx;
    }
}
