package dev.liambloom.tests.bjp.shared;

public interface Logger {
    void log(LogKind logKind, String msg, Object... args);

}
