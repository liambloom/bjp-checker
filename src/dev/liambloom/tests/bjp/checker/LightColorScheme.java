package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

class LightColorScheme implements ColorScheme {
    LightColorScheme() {}

    @Override
    public Color getBackground() {
        return Color.WHITE;
    }

    @Override
    public Color getForeground() {
        return Color.BLACK;
    }

    public static void foo() {}
}
