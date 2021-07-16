package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

public class LightColorScheme extends ColorScheme {
    @Override
    public Color getBackground() {
        return Color.WHITE;
    }

    @Override
    public Color getForeground() {
        return Color.BLACK;
    }

//    @Override
//    public Color getGray(int brightness) {
//        return Color.hsb(0, 0, brightness * 0.01);
//    }
}
