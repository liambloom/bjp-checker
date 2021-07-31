package dev.liambloom.tests.bjp.gui;

import javafx.scene.paint.Color;

class LightColorScheme implements ColorScheme {
    protected LightColorScheme() {}

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
    public Color getAltBackgroundHover() {
        return Color.gray(0.75);
    }

    @Override
    public Color getTitle() {
        return Color.gray(0.35);
    }

    @Override
    public Color getSubTitle() {
        return Color.gray(0.45);
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
