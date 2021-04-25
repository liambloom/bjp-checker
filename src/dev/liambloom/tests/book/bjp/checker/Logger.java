package dev.liambloom.tests.book.bjp.checker;

public interface Logger {
    Logger notice(String msg, Object... args);
    Logger warn(String msg, Object... args);
    Logger error(String msg, Object... args);
}
