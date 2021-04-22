package dev.liambloom.tests.book.bjp.checker;

public class Result<T extends ResultVariant> {
    public final String name;
    public final T variant;
    public final Throwable error;

    public Result(String name, T variant) {
        this(name, variant, null);
    }

    public Result(String name, T variant, Throwable error) {
        this.name = name;
        this.variant = variant;
        this.error = error;
    }
}
