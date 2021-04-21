package dev.liambloom.tests.book.bjp.checker;

public interface Logger {
    void notice(String msg, Object... args);
    void warn(String msg, Object... args);
    void error(String msg, Object... args);
}
