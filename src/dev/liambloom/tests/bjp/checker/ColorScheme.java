package dev.liambloom.tests.bjp.checker;

import javafx.beans.Observable;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.prefs.Preferences;

public abstract class ColorScheme {
    private static final SimpleIntegerProperty schemeIndex = new SimpleIntegerProperty();
    private static final ObjectBinding<ColorScheme> scheme = new ObjectBinding<>() {
        { bind(schemeIndex); }

        @Override
        protected ColorScheme computeValue() {
            return allSchemes.get(schemeIndex.get());
        }
    };
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
            String name = scheme.get("name", null);
            boolean useGammaBlending = scheme.getBoolean("useGammaBlending", false);
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

                @Override
                public boolean useGammaBlending() {
                    return useGammaBlending;
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
    public static int getIndex() { return schemeIndex.get(); }

    public static void set(int scheme) {
        colorSchemePrefs.putInt("current", scheme);
        schemeIndex.set(scheme);
    }

    public abstract Color getBackground();
    public abstract Color getForeground();
    public abstract String name();

    /**
     * Determines whether gamma adjustments are taken into account when calculating
     * gray values for this color scheme. Default value: {@code false}.
     *
     * Finding blends of computer colors is complicated. Java stores colors as sRGB
     * colors, which are space efficient and meant to store colors that we can
     * more easily distinguish with more precision. This is good, but it can cause
     * problems when blending colors, which you can find out more about from
     * <a href="https://youtu.be/LKnqECcg6Gw">this</a> video. If this method is
     * overridden to return {@code true} then the more mathematically accurate
     * method of calculating color gradients, as explained in the video, is used.
     * This method can, however, give unappealing results for blending colors that
     * have a dramatically different value of R+G+B (such as black and white).
     *
     * Generally, if your foreground and background colors have similar R+G+B values
     * (such as red and green), you probably want to override this method to return
     * {@code true}, but if your foreground and background colors have very different
     * R+G+B values (such as black and white), you probably want to have the method
     * return {@code false} (which happens if you don't override it)
     *
     * @return Whether or not this color scheme should use gamma blending.
     */
    public boolean useGammaBlending() {
        return false;
    }


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
                    final double m = brightness * 0.01;
                    final Color fg, bg;
                    if (s.useGammaBlending()) {
                        fg = inverseSRGBCompand(s.getForeground());
                        bg = inverseSRGBCompand(s.getBackground());
                    }
                    else {
                        fg = s.getForeground();
                        bg = s.getBackground();
                    }

                    Color mixed = new Color(
                            m * fg.getRed() + (1-m) * bg.getRed(),
                            m * fg.getGreen() + (1-m) * bg.getGreen(),
                            m * fg.getBlue() + (1-m) * bg.getBlue(),
                            m * (fg.getOpacity() - bg.getOpacity()) + bg.getOpacity()
                    );

                    if (s.useGammaBlending())
                        mixed = sRGBCompand(mixed);

                    return mixed;
                }
            };
        }

        return grays[brightness];
    }

    private static Color inverseSRGBCompand(Color c) {
        return new Color(
                inverseSRGBCompandChanel(c.getRed()),
                inverseSRGBCompandChanel(c.getGreen()),
                inverseSRGBCompandChanel(c.getBlue()),
                c.getOpacity()
        );
    }

    private static double inverseSRGBCompandChanel(double c) {
        return c > 0.04045 ? Math.pow((c + 0.055) / 1.055, 2.4) : c / 12.92;
    }

    private static Color sRGBCompand(Color c) {
        return new Color(
                sRGBCompandChanel(c.getRed()),
                sRGBCompandChanel(c.getGreen()),
                sRGBCompandChanel(c.getBlue()),
                c.getOpacity()
        );
    }

    private static double sRGBCompandChanel(double c) {
        return c > 0.0031308 ? 1.055 * Math.pow(c, 1 / 2.4) - 0.055 : c * 11.92;
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
        schemeStore.putBoolean("useGammaBlending", s.useGammaBlending());
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
