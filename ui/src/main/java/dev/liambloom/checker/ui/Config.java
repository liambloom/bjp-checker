package dev.liambloom.checker.ui;

import java.util.prefs.Preferences;

public final class Config {
    private static final Preferences prefs = Preferences.userNodeForPackage(Config.class).node("config");

    public enum Property {
        BOOK;
    }

    private Config() {
    }

    public static boolean propertyExists(String name) {
        try {
            Property.valueOf(name);
            return true;
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String get(String s) {
        return get(Property.valueOf(s));
    }

    public static String get(Property p) {
        return prefs.get(p.name(), null);
    }

    public static void set(String p, String v) {
        set(Property.valueOf(p), v);
    }

    public static void set(Property p, String v) {
        prefs.put(p.name(), v);
    }

    public static void unset(String p) {
        unset(Property.valueOf(p));
    }

    public static void unset(Property p) {
        prefs.remove(p.name());
    }
}
