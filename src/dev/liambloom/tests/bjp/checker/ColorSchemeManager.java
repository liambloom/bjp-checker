package dev.liambloom.tests.bjp.checker;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

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

    public static ObjectProperty<ColorScheme> getColorSchemeProperty() {
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

    private static final ObjectBinding<Color> menuTitleProperty = new MethodBinding<>(scheme, ColorScheme::getMenuTitle);
    public static ObjectBinding<Color> getMenuTitleProperty() {
        return menuTitleProperty;
    }

    private static final ObjectBinding<Color> grayscaleIconProperty = new MethodBinding<>(scheme, ColorScheme::getGrayscaleIcon);
    public static ObjectBinding<Color> getGrayscaleIconProperty() {
        return grayscaleIconProperty;
    }

    // ----------------------------------------------------------------------

    static {
        scheme.addListener((_1, _2, newValue) -> App.prefs().put("colorScheme", newValue.getClass().getName()));

        try {
            String schemeName = App.prefs().get("colorScheme", LightColorScheme.class.getName());
            setColorScheme(switch (schemeName) {
                case "dev.liambloom.tests.bjp.checker.LightColorScheme" -> getLightColorScheme();
                case "dev.liambloom.tests.bjp.checker.DarkColorScheme" -> getDarkColorScheme();
                default -> (ColorScheme) ColorSchemeManager.class.getClassLoader().loadClass(schemeName).getDeclaredConstructor().newInstance();
            });
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // TODO: maybe produce a warning?
            App.prefs().remove("colorScheme");
            try {
                App.createLogFile(e);
            } catch (IOException ignored) { }
            setColorScheme(getLightColorScheme());
        }
    }
}
