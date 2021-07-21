package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

class DarkColorScheme implements ColorScheme {
    DarkColorScheme() {}

    @Override
    public javafx.scene.paint.Color getBackground() {
        return Color.BLACK;
    }

    @Override
    public javafx.scene.paint.Color getForeground() {
        return Color.WHITE;
    }
}
