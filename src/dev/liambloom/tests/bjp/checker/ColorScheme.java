package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

public interface ColorScheme {
    Color getBackground();
    Color getForeground();
    Color getAltBackground();
    Color getMenuTitle();
    Color getGrayscaleIcon(); // FIXME: I don't love this name
}
