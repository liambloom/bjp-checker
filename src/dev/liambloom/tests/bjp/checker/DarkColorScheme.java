package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

public class DarkColorScheme extends ColorScheme {
    @Override
    public javafx.scene.paint.Color getBackground() {
        return Color.BLACK;
    }

    @Override
    public javafx.scene.paint.Color getForeground() {
        return Color.WHITE;
    }

    @Override
    public javafx.scene.paint.Color getGray() {
        return Color.LIGHTGRAY;
    }
}
