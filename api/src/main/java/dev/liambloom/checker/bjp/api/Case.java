package dev.liambloom.checker.bjp.api;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A utility class used to convert between cases. Currently only works
 * on english.
 */
public enum Case {
    /**
     * Capitalizes the first letter of every word except the first. thisIsAnExampleOfCamelCase
     */
    CAMEL,

    /**
     * Capitalizes the first letter of every word. ThisIsAnExampleOfPascalCase
     */
    PASCAL,

    /**
     * All lowercase, words separated by underscores. this_is_an_example_of_snake_case
     */
    SNAKE,

    /**
     * All uppercase, words separated by underscores. THIS_IS_AN_EXAMPLE_OF_CONST_CASE
     */
    CONST,

    /**
     * All lowercase, words separated by a single space. this is an example of space case
     */
    SPACE,

    /**
     * Capitalizes the first letter of every word, separating words with spaces. This Is An Example Of Title Case
     */
    TITLE,

    /**
     * Capitalizes the first character of the string, separates words with spaces. This is an example of sentence case
     */
    SENTENCE;

    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    public static String convert(String s, Case c) {
        String[] words = s.split(
            WHITESPACE.matcher(s).find() ? "\\s+"
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
            case SPACE, SENTENCE -> {
                String r = String.join(" ", words).toLowerCase(Locale.ENGLISH);
                if (c == SPACE || r.isEmpty())
                    yield r;
                else
                    yield Character.toUpperCase(r.charAt(0)) + r.substring(1);
            }
            case CAMEL, PASCAL, TITLE -> {
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
                    if (i > 0 && c == TITLE)
                        builder.append(' ');
                    builder.append(chars);
                }
                yield builder.toString();
            }
        };
    }
}