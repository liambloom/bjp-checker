package dev.liambloom.checker.ui;

import dev.liambloom.checker.books.BookParser;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

public final class Parsers {
    private static final Preferences prefs = Preferences.userNodeForPackage(Books.class).node("books");
    private static final Map<String, BookParser> loadedParsers = new WeakHashMap<>();

    private Parsers() {

    }


}
