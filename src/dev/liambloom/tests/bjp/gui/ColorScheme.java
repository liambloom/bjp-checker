package dev.liambloom.tests.bjp.gui;

import javafx.scene.paint.Color;

public interface ColorScheme {
    Color getBackground();
    Color getForeground();
    Color getAltBackground();
    Color getAltBackgroundHover();
    Color getTitle();
    Color getSubTitle();
    Color getGrayscaleIcon(); // FIXME: I don't love this name
}
