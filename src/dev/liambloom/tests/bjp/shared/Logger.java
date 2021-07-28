package dev.liambloom.tests.bjp.shared;

import java.util.stream.Stream;

public interface Logger {
    void log(LogKind logKind, String msg, Object... args);

    enum LogKind {
        FATAL_ERROR,
        ERROR,
        WARN,
        NOTICE,
        OTHER
    }
}
