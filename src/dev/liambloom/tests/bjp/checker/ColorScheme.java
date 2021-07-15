package dev.liambloom.tests.bjp.checker;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;
import java.util.prefs.Preferences;

public abstract class ColorScheme {
    private static ColorScheme scheme = null;
    private static final ReadOnlyObjectWrapper<Color> background = new ReadOnlyObjectWrapper<>();
    private static final ReadOnlyObjectWrapper<Color> foreground = new ReadOnlyObjectWrapper<>();
    private static final ReadOnlyObjectWrapper<Function<Integer, Color>> gray = new ReadOnlyObjectWrapper<>();
    private static final Preferences prefs = Preferences.userNodeForPackage(ColorScheme.class);

    static {
        // TODO (maybe): Choose default with: https://github.com/Dansoftowner/jSystemThemeDetector
        try {
            set(prefs.get("colorScheme", LightColorScheme.class.getName()));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            prefs.remove("colorScheme");
            try {
                App.createLogFile(e);
            } catch (IOException ignored) { }
            set((ColorScheme) null);
        }
    }

    public static ColorScheme get() {
        return scheme;
    }

    public static void set(ColorScheme scheme) {
        if (ColorScheme.scheme == null) {
            if (scheme == null)
                scheme = new LightColorScheme();
        }
        else {
            prefs.put("colorScheme", scheme.getClass().getName());
        }
        ColorScheme.scheme = scheme;
        background.set(scheme.getBackground());
        foreground.set(scheme.getForeground());
        gray.set(scheme::getGray);
    }

    public static void set(String scheme) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        try {
            set((ColorScheme) ColorScheme.class.getClassLoader().loadClass(scheme).getDeclaredConstructor().newInstance());
        }
        catch (NoSuchMethodException e) {
            throw new InstantiationException("Class " + scheme + " does not have a no argument constructor");
        }
    }

    // I can't decide if I want to make these two methods final
    public Color getBackground() {
        return getGray(100);
    }

    public Color getForeground() {
        return getGray(0);
    }

    public abstract Color getGray(int brightness);


    public static ReadOnlyObjectProperty<Color> getBackgroundProperty() {
        return background.getReadOnlyProperty();
    }

    public static ReadOnlyObjectProperty<Color> getForegroundProperty() {
        return foreground.getReadOnlyProperty();
    }

    public static ObjectBinding<Color> getGrayProperty(int brightness) {
        return new ObjectBinding<>() {
            { bind(gray); }

            @Override
            protected Color computeValue() {
                return gray.get().apply(brightness);
            }
        };
    }
}
