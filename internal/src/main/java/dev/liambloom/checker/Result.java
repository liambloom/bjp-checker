package dev.liambloom.checker;

import dev.liambloom.checker.internal.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public record Result<T extends Result.Status>(String name, T status, ReLogger logs, ByteArrayOutputStream consoleOutput, List<? extends Result<? extends T>> subResults) {
    public Result(String name, T status, ReLogger logs, ByteArrayOutputStream consoleOutput) {
        this(name, status, logs, consoleOutput, defaultSubResults());
    }

    public Result(String name, T status, ReLogger logs, List<? extends Result<? extends T>> subResults) {
        this(name, status, logs, defaultConsole(), subResults);
    }

    public Result(String name, T status, ByteArrayOutputStream consoleOutput, List<? extends Result<? extends T>> subResults) {
        this(name, status, defaultLogs(), consoleOutput, subResults);
    }

    public Result(String name, T status, ReLogger logs) {
        this(name, status, logs, defaultConsole(), defaultSubResults());
    }

    public Result(String name, T status, ByteArrayOutputStream consoleOutput) {
        this(name, status, defaultLogs(), consoleOutput, defaultSubResults());
    }

    public Result(String name, T status, List<? extends Result<? extends T>> subResults) {
        this(name, status, defaultLogs(), defaultConsole(), subResults);
    }

    public Result(String name, T status) {
        this(name, status, defaultLogs(), defaultConsole(), defaultSubResults());
    }

    private static ReLogger defaultLogs() {
        return new ReLogger(Util.generateLoggerName());
    }

    private static ByteArrayOutputStream defaultConsole() {
        return new ByteArrayOutputStream();
    }

    private static <T extends Status> List<? extends Result<? extends T>> defaultSubResults() {
        return Collections.emptyList();
    }

    public interface Status {
        Color color();
        String name();
    }
}
