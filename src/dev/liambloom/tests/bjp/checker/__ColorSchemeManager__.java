package dev.liambloom.tests.bjp.checker;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class __ColorSchemeManager__ {
    private __ColorSchemeManager__() {}

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
        App.prefs().put("colorScheme", scheme.getClass().getName());
        __ColorSchemeManager__.scheme.set(scheme);
    }

    // ----------------------------------------------------------------------

    private static final ObjectBinding<javafx.scene.paint.Color> foregroundProperty = new ObjectBinding<>() {
        { bind(scheme); }

        @Override
        protected javafx.scene.paint.Color computeValue() {
            return scheme.get().getForeground();
        }
    };

    public static ObjectBinding<Color> getForegroundProperty() {
        return foregroundProperty;
    }

    private static final ObjectBinding<javafx.scene.paint.Color> backgroundProperty = new ObjectBinding<>() {
        { bind(scheme); }

        @Override
        protected Color computeValue() {
            return scheme.get().getBackground();
        }
    };

    public static ObjectBinding<Color> getBackgroundProperty() {
        return backgroundProperty;
    }

    // ----------------------------------------------------------------------

    static {
        try {
            scheme.set((ColorScheme) ColorScheme.class.getClassLoader().loadClass(App.prefs().get("colorScheme", LightColorScheme.class.getName()))
                    .getDeclaredConstructor().newInstance());
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            App.prefs().remove("colorScheme");
            try {
                App.createLogFile(e);
            } catch (IOException ignored) { }
            setColorScheme(new LightColorScheme());
        }
    }
}
