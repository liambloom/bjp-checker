package dev.liambloom.tests.book.bjp.checker;

import java.io.ByteArrayOutputStream;

public class Result<T extends ResultVariant> {
    public final String name;
    public final T variant;
    public final Throwable error;
    public final ByteArrayOutputStream console;

    public Result(String name, T variant) {
        this(name, variant, null, null);
    }

    public Result(String name, T variant, Throwable error) {
        this(name, variant, error, null);
    }

    public Result(String name, T variant, ByteArrayOutputStream console) {
        this(name, variant, null, console);
    }

    public Result(String name, T variant, Throwable error, ByteArrayOutputStream console) {
        if ((error == null) != variant.isOk())
            throw new IllegalArgumentException(String.format("Error %s null but variant %s an error",
                    error == null ? "is" : "isn't", variant.isError() ? "is" : "isn't"));
        this.name = name;
        this.variant = variant;
        this.error = error;
        this.console = console;
    }
}
