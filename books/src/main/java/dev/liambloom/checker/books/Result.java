package dev.liambloom.checker.books;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record Result<T extends Result.Status>(String name, T status, Optional<ReLogger> logs, Optional<ByteArrayOutputStream> consoleOutput, List<? extends Result<? extends T>> subResults) {
    public Result(String name, T status, ReLogger logs, ByteArrayOutputStream consoleOutput, List<? extends Result<? extends T>> subResults) {
        this(name, status, Optional.of(logs), Optional.of(consoleOutput), subResults);
    }

    public Result(String name, T status, ReLogger logs, ByteArrayOutputStream consoleOutput) {
        this(name, status, Optional.of(logs), Optional.of(consoleOutput), Collections.emptyList());
    }

    public Result(String name, T status, ReLogger logs, List<? extends Result<? extends T>> subResults) {
        this(name, status, Optional.of(logs), Optional.empty(), subResults);
    }

    public Result(String name, T status, ByteArrayOutputStream consoleOutput, List<? extends Result<? extends T>> subResults) {
        this(name, status, Optional.empty(), Optional.of(consoleOutput), subResults);
    }

    public Result(String name, T status, ReLogger logs) {
        this(name, status, Optional.of(logs), Optional.empty(), Collections.emptyList());
    }

    public Result(String name, T status, ByteArrayOutputStream consoleOutput) {
        this(name, status, Optional.empty(), Optional.of(consoleOutput), Collections.emptyList());
    }

    public Result(String name, T status, List<? extends Result<? extends T>> subResults) {
        this(name, status, Optional.empty(), Optional.empty(), subResults);
    }

    public Result(String name, T status) {
        this(name, status, Optional.empty(), Optional.empty(), Collections.emptyList());
    }

    public interface Status {
        Color color();
        String name();
    }
}
