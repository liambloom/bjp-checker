package dev.liambloom.tests.bjp.checker;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.prefs.Preferences;

public abstract class ColorScheme {
    private static final SimpleObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>();
    private static final ArrayList<ColorScheme> allSchemes = new ArrayList<>();
    private static final Set<String> schemeNames = new HashSet<>();
    private static final Preferences colorSchemePrefs = App.prefs().node("colorScheme");
    private static final ColorScheme[] DEFAULT_SCHEMES = {
            new ColorScheme() {
                @Override
                public Color getBackground() {
                    return Color.WHITE;
                }

                @Override
                public Color getForeground() {
                    return Color.BLACK;
                }

                @Override
                public String name() {
                    return "Light";
                }
            },
            new ColorScheme() {
                @Override
                public Color getBackground() {
                    return Color.BLACK;
                }

                @Override
                public Color getForeground() {
                    return Color.WHITE;
                }

                @Override
                public String name() {
                    return "Dark";
                }
            }
    };
    private static final int RESERVED_SCHEMES = DEFAULT_SCHEMES.length;


    static {
        int schemeCount = colorSchemePrefs.getInt("count", RESERVED_SCHEMES);
        allSchemes.ensureCapacity(schemeCount);

        allSchemes.addAll(Arrays.asList(DEFAULT_SCHEMES));

        // load schemes
        for (int i = RESERVED_SCHEMES; i < schemeCount; i++) {
            Preferences scheme = colorSchemePrefs.node("userDefined/" + i);
            byte[] err = new byte[0];
            String name = scheme.get("name", null);
            byte[] bgBytes = scheme.getByteArray("background", null);
            byte[] fgBytes = scheme.getByteArray("foreground", null);
            if (name == null || bgBytes == null || fgBytes == null)
                throw new UserErrorException("Unable to load scheme " + (name == null ? "[name not found]" : name));
            Color background = Color.rgb(bgBytes[0], bgBytes[1], bgBytes[2], (double) bgBytes[3] / 0xff);
            Color foreground = Color.rgb(fgBytes[0], fgBytes[1], fgBytes[2], (double) fgBytes[3] / 0xff);
            allSchemes.add(new ColorScheme() {
                @Override
                public Color getBackground() {
                    return background;
                }

                @Override
                public Color getForeground() {
                    return foreground;
                }

                @Override
                public String name() {
                    return name;
                }
            });
        }

        for (ColorScheme s : allSchemes) {
            schemeNames.add(s.name());
        }

        set(colorSchemePrefs.getInt("current", 0));

        // TODO (maybe): Choose default with: https://github.com/Dansoftowner/jSystemThemeDetector
//        try {
//            set(App.prefs().get("colorScheme", new LightColorScheme()));
//        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
//            App.prefs().remove("colorScheme");
//            try {
//                App.createLogFile(e);
//            } catch (IOException ignored) { }
//            set((ColorScheme) null);
//        }
    }

    /**
     * Grays is an array of lazily instantiated object bindings. Each binding
     * corresponds to a shade of gray, calculated on a gradient based on the color
     * scheme, such that gray 0 is the background color and gray 100 is the
     * foreground color
     */
    @SuppressWarnings("unchecked")
    private static final ObjectBinding<Color>[] grays = (ObjectBinding<Color>[]) new ObjectBinding[101];

    public static ColorScheme get() {
        return scheme.get();
    }

    public static void set(int scheme) {
        colorSchemePrefs.putInt("current", scheme);
        ColorScheme.scheme.set(allSchemes.get(scheme));
    }

    public abstract Color getBackground();
    public abstract Color getForeground();
    public abstract String name();


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

                    return new Color(
                            r * (fg.getRed() - bg.getRed()) + bg.getRed(),
                            r * (fg.getGreen() - bg.getGreen()) + bg.getGreen(),
                            r * (fg.getBlue() - bg.getBlue()) + bg.getBlue(),
                            r * (fg.getOpacity() - bg.getOpacity()) + bg.getOpacity()
                    );
                }
            };
        }

        return grays[brightness];
    }

    public static List<ColorScheme> getColorSchemes() {
        return Collections.unmodifiableList(allSchemes);
    }

    protected static void createUserColorScheme(ColorScheme s) {
        if (schemeNames.contains(s.name()))
            throw new IllegalArgumentException("Color scheme name " + s.name() + " is already in use");
        if (s.name().contains("/"))
            throw new IllegalArgumentException("Illegal scheme name: " + s.name());
        allSchemes.add(s);
        colorSchemePrefs.putInt("count", colorSchemePrefs.getInt("count", RESERVED_SCHEMES) + 1);
        Preferences schemeStore = colorSchemePrefs.node("userDefined/" + (allSchemes.size() - 1));
        schemeStore.put("name", s.name());
        Color bg = s.getBackground();
        schemeStore.putByteArray("background", new byte[]{
                (byte) (bg.getRed() * 0xff),
                (byte) (bg.getGreen() * 0xff),
                (byte) (bg.getBlue() * 0xff),
                (byte) (bg.getOpacity() * 0xff)
        });
        Color fg = s.getForeground();
        schemeStore.putByteArray("foreground", new byte[]{
                (byte) (fg.getRed() * 0xff),
                (byte) (fg.getGreen() * 0xff),
                (byte) (fg.getBlue() * 0xff),
                (byte) (fg.getOpacity() * 0xff)
        });
    }
}
