package dev.liambloom.tests.bjp.checker;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;
import java.util.prefs.Preferences;

public abstract class ColorScheme {
    private static final SimpleObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>();

    /**
     * Grays is an array of lazily instantiated object bindings. Each binding
     * corresponds to a shade of gray, calculated on a gradient based on the color
     * scheme, such that gray 0 is the background color and gray 100 is the
     * foreground color
     */
    @SuppressWarnings("unchecked")
    private static final ObjectBinding<Color>[] grays = (ObjectBinding<Color>[]) new ObjectBinding[101];

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
        return scheme.get();
    }

    public static void set(ColorScheme scheme) {
        if (scheme == null) {
            if (ColorScheme.scheme.get() == null)
                scheme = new LightColorScheme();
            else
                throw new NullPointerException("scheme may not be null");
        }

        prefs.put("colorScheme", scheme.getClass().getName());
        ColorScheme.scheme.set(scheme);
    }

    public static void set(String scheme) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        try {
            set((ColorScheme) ColorScheme.class.getClassLoader().loadClass(scheme).getDeclaredConstructor().newInstance());
        }
        catch (NoSuchMethodException e) {
            throw new InstantiationException("Class " + scheme + " does not have a no argument constructor");
        }
    }

    public abstract Color getBackground();
    public abstract Color getForeground();


    public static ObjectBinding<Color> getBackgroundProperty() {
        return getGrayProperty(0);
    }

    public static ObjectBinding<Color> getForegroundProperty() {
        return getGrayProperty(100);
    }

    public static ObjectBinding<Color> getGrayProperty(int brightness) {
        if (grays[brightness] == null) {
            grays[brightness] = new ObjectBinding<>() {
                { bind(scheme); }

                @Override
                protected Color computeValue() {
                    final ColorScheme s = scheme.get();
                    final Color fg = s.getForeground();
                    final Color bg = s.getBackground();
                    final double r = brightness * 0.01;

                    Color c = new Color(
                            r * (fg.getRed() - bg.getRed()) + bg.getRed(),
                            r * (fg.getGreen() - bg.getGreen()) + bg.getGreen(),
                            r * (fg.getBlue() - bg.getBlue()) + bg.getBlue(),
                            r * (fg.getOpacity() - bg.getOpacity()) + bg.getOpacity()
                    );

                    return c;
                }
            };
        }

        return grays[brightness];
    }
}
