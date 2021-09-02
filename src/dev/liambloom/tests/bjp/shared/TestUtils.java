package dev.liambloom.tests.bjp.shared;

import java.lang.reflect.*;
import java.util.regex.Pattern;

final class TestUtils {
    private TestUtils() {}

    private static final Pattern LINE_SEPARATOR = Pattern.compile("\\r|\\r?\\n");

    public static String normalizeLineSeparators(String s) {
        return LINE_SEPARATOR.matcher(s).replaceAll(System.lineSeparator());
    }

    public static String executableToString(Executable e) {
        StringBuilder builder = new StringBuilder()
                .append(e.getDeclaringClass().getName())
                .append('.')
                .append(e.getName())
                .append('(');
        for (Type t : e.getGenericParameterTypes())
            builder.append(t.getTypeName());
        builder.append(')');
        return builder.toString();
    }

    public static String getAccessabilityModifierName(int modifier) {
        if (Modifier.isPublic(modifier))
            return "public";
        else if (Modifier.isProtected(modifier))
            return "protected";
        else if (Modifier.isPrivate(modifier))
            return "private";
        else
            return "package-private";
    }

    public static String getAccessabilityModifierName(Member m) {
        return getAccessabilityModifierName(m.getModifiers());
    }
}
