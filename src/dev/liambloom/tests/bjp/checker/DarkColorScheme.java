package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

public class DarkColorScheme extends ColorScheme {
//    @Override
//    public javafx.scene.paint.Color getBackground() {
//        return Color.BLACK;
//    }
//
//    @Override
//    public javafx.scene.paint.Color getForeground() {
//        return Color.WHITE;
//    }

    @Override
    public javafx.scene.paint.Color getGray(int brightness) {
        return Color.hsb(0, 0, 1 - brightness * 0.01);
    }
}
