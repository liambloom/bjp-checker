package dev.liambloom.tests.bjp.gui;

import javafx.scene.paint.Color;

class DarkColorScheme implements ColorScheme {
    protected DarkColorScheme() {}

    @Override
    public javafx.scene.paint.Color getBackground() {
        return Color.BLACK;
    }

    @Override
    public javafx.scene.paint.Color getForeground() {
        return Color.WHITE;
    }

    @Override
    public Color getAltBackground() {
        return Color.gray(0.15);
    }

    @Override
    public Color getAltBackgroundHover() {
        return Color.gray(0.25);
    }

    @Override
    public Color getTitle() {
        return Color.gray(0.65);
    }

    @Override
    public Color getSubTitle() {
        return Color.gray(0.55);
    }

    @Override
    public Color getGrayscaleIcon() {
        return Color.gray(0.3);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DarkColorScheme;
    }
}
