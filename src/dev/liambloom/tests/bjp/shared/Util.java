package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class Util {
    private Util() {}

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
        Type[] args = e.getGenericParameterTypes();
        for (int i = 0; i < args.length; i++) {
            if (i + 1 == args.length && e.isVarArgs()) {
                builder.append(args[i].getTypeName())
                        .replace(builder.length() - 2, builder.length(), "...");
            }
            else
                builder.append(args[i].getTypeName());
        }
        builder.append(')');
        return builder.toString();
    }

    public static Stream<Node> streamNodeList(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength())
                .parallel()
                .mapToObj(nodeList::item);
    }

    public static String getAccessibilityModifierName(int modifier) {
        if (Modifier.isPublic(modifier))
            return "public";
        else if (Modifier.isProtected(modifier))
            return "protected";
        else if (Modifier.isPrivate(modifier))
            return "private";
        else
            return "package-private";
    }

    public static String getAccessibilityModifierName(Member m) {
        return getAccessibilityModifierName(m.getModifiers());
    }
}
