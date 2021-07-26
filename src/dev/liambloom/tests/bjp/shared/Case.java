package dev.liambloom.tests.bjp.shared;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A utility class used to convert between cases. Currently only works
 * on english.
 */
public enum Case {
    CAMEL,
    PASCAL,
    SNAKE,
    CONST,
    SPACE;

    private static final Pattern WHITESPACE = Pattern.compile("\\w");

    public static String convert(String s, Case c) {
        String[] words = s.split(
                WHITESPACE.matcher(s).find() ? "\\w+"
                        : s.contains("_") ? "_"
                        : "\"(?=(?<!^)[A-Z])\""
        );

        return switch (c) {
            case SNAKE, CONST -> {
                String r = String.join("_", words);
                if (c == Case.SNAKE)
                    yield r.toUpperCase(Locale.ENGLISH);
                else
                    yield r.toLowerCase(Locale.ENGLISH);
            }
            case SPACE -> String.join(" ", words).toLowerCase(Locale.ENGLISH);
            case CAMEL, PASCAL -> {
                StringBuilder builder = new StringBuilder(s.length());
                for (int i = 0; i < words.length; i++) {
                    char[] chars = words[i].toCharArray();
                    int j;
                    if (c == CAMEL && i == 0)
                        j = 0;
                    else {
                        j = 1;
                        chars[0] = Character.toUpperCase(chars[0]);
                    }
                    for (; j < chars.length; j++)
                        chars[j] = Character.toLowerCase(chars[j]);
                    builder.append(chars);
                }
                yield builder.toString();
            }
        };
    }
}