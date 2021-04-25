package dev.liambloom.tests.book.bjp.checker;

import static org.fusesource.jansi.Ansi.Color;

public interface ResultVariant {
    boolean isOk();
    default boolean isError() {
        return !isOk();
    }

    default Color color() {
        return isOk() ? Color.GREEN : Color.RED;
    }

    boolean printStackTrace();
}
