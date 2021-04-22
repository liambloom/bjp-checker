package dev.liambloom.tests.book.bjp.checker;

public interface ResultVariant {
    boolean isOk();
    default boolean isError() {
        return !isOk();
    }
}
