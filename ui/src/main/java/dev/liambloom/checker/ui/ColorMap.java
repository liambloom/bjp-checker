package dev.liambloom.checker.ui;

import javafx.scene.paint.Color;//dev.liambloom.checker.books.Color;

public final class ColorMap {
    private ColorMap() {
    }

    public static String ansi(dev.liambloom.checker.books.Color color) {
        return switch (color) {
            case RED -> "31";
            case GREEN -> "32";
            case YELLOW -> "33";
            case BLUE -> "34";
            case MAGENTA -> "35";
            case CYAN -> "36";
            case GRAY -> "38;5;8";
            case RESET -> "0";
        };
    }

    public static Color jfx(dev.liambloom.checker.books.Color color) {
        return switch (color) {
            case RED -> Color.RED;
            case GREEN -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
            case BLUE -> Color.BLUE;
            case MAGENTA -> Color.MAGENTA;
            case CYAN -> Color.CYAN;
            case GRAY -> Color.GRAY;
            case RESET -> null;
        };
    }
}
