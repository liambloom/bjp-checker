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

    @Override
    public Color getAltBackground() {
        return Color.gray(0.85);
    }

    @Override
    public Color getMenuTitle() {
        return Color.gray(0.35);
    }

    @Override
    public Color getGrayscaleIcon() {
        return Color.gray(0.7);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LightColorScheme;
    }
}
