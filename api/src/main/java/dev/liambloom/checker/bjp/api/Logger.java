package dev.liambloom.checker.bjp.api;

public interface Logger {
    void log(LogKind logKind, String msg, Object... args);
}
