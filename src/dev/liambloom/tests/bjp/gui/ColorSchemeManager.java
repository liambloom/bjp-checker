package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.App;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class ColorSchemeManager {
    private ColorSchemeManager() {}

    private static final ColorScheme light = new LightColorScheme();

    public static ColorScheme getLightColorScheme() {
        return light;
    }

    private static final ColorScheme dark = new DarkColorScheme();

    public static ColorScheme getDarkColorScheme() {
        return dark;
    }

    // ----------------------------------------------------------------------

    private static final SimpleObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>();

    public static ColorScheme getColorScheme() {
        return scheme.get();
    }

    public static void setColorScheme(ColorScheme scheme) {
        ColorSchemeManager.scheme.set(scheme);
    }

    public static ObjectProperty<ColorScheme> colorSchemeProperty() {
        return scheme;
    }

    // ----------------------------------------------------------------------

    private static final ObjectBinding<Color> foregroundProperty = new MethodBinding<>(scheme, ColorScheme::getForeground);
    public static ObjectBinding<Color> getForegroundProperty() {
        return foregroundProperty;
    }

    private static final ObjectBinding<Color> backgroundProperty = new MethodBinding<>(scheme, ColorScheme::getBackground);
    public static ObjectBinding<Color> getBackgroundProperty() {
        return backgroundProperty;
    }

    private static final ObjectBinding<Color> altBackgroundProperty = new MethodBinding<>(scheme, ColorScheme::getAltBackground);
    public static ObjectBinding<Color> getAltBackgroundProperty() {
        return altBackgroundProperty;
    }

    private static final ObjectBinding<Color> altBackgroundHoverProperty = new MethodBinding<>(scheme, ColorScheme::getAltBackgroundHover);
    public static ObjectBinding<Color> getAltBackgroundHoverProperty() {
        return altBackgroundHoverProperty;
    }

    private static final ObjectBinding<Color> titleProperty = new MethodBinding<>(scheme, ColorScheme::getTitle);
    public static ObjectBinding<Color> getTitleProperty() {
        return titleProperty;
    }

    private static final ObjectBinding<Color> subTitleProperty = new MethodBinding<>(scheme, ColorScheme::getSubTitle);
    public static ObjectBinding<Color> getSubTitleProperty() {
        return subTitleProperty;
    }

    private static final ObjectBinding<Color> grayscaleIconProperty = new MethodBinding<>(scheme, ColorScheme::getGrayscaleIcon);
    public static ObjectBinding<Color> getGrayscaleIconProperty() {
        return grayscaleIconProperty;
    }

    // ----------------------------------------------------------------------

    static {
        scheme.addListener((_1, _2, newValue) -> App.prefs().put("colorScheme", newValue.getClass().getName()));

        String schemeName = App.prefs().get("colorScheme", LightColorScheme.class.getName());
        setColorScheme(switch (schemeName) {
            case "dev.liambloom.tests.bjp.gui.LightColorScheme" -> getLightColorScheme();
            case "dev.liambloom.tests.bjp.gui.DarkColorScheme" -> getDarkColorScheme();
            default -> {
                try {
                    yield (ColorScheme) ColorSchemeManager.class.getClassLoader().loadClass(schemeName).getDeclaredConstructor().newInstance();
                }
                catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    App.prefs().remove("colorScheme");
                    try {
                        App.createLogFile(e);
                    } catch (IOException ignored) { }
                    yield getLightColorScheme();
                }
            }
        });
    }
}
