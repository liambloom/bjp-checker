package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

@Managed
public interface ColorScheme {
    Color getBackground();
    Color getForeground();
    Color getAltBackground();
}
