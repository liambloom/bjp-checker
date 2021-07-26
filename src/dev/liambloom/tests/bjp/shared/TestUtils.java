package dev.liambloom.tests.bjp.shared;

import java.util.regex.Pattern;

public class TestUtils {
    private TestUtils() {}

    private static final Pattern LINE_SEPARATOR = Pattern.compile("\\r|\\r?\\n");

    public static String normalizeLineSeparators(String s) {
        return LINE_SEPARATOR.matcher(s).replaceAll(System.lineSeparator());
    }
}
