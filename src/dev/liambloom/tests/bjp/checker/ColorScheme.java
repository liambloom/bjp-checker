package dev.liambloom.tests.bjp.checker;

import javafx.scene.paint.Color;

@Managed(singletons = {
        LightColorScheme.class,
        DarkColorScheme.class
})
public interface ColorScheme {
    Color getBackground();
    Color getForeground();
    //Color getAltBackground();

    //private static ColorScheme light() { return null; }
}
