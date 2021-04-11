package dev.liambloom.tests.book.bjp3;

import java.util.Optional;

// This is based on rust's core::result::Result
final class Result<T, E extends Throwable> /* extends SuperResult<T, E> */ {
    @FunctionalInterface
    public interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    private final T ok;
    private final E error;

    private Result(T ok, E error) {
        this.ok = ok;
        this.error = error;
    }

    public static <T, E extends Throwable> Result<T, E> ok(T ok) {
        return new Result<>(ok, null);
    }

    public static <T, E extends Throwable> Result<T, E> error(E error) {
        if (error == null)
            throw new NullPointerException("You cannot have a null error");
        return new Result<>(null, error);
    }

    public boolean isOk() {
        return error == null;
    }

    public boolean isError() {
        return !isOk();
    }

    public Optional<T> ok() {
        return Optional.ofNullable(ok);
    }

    public Optional<E> error() {
        return Optional.ofNullable(error);
    }

    // TODO: make some sort of convenience method for passing in a Supplier

    public static <T> Result<T, Throwable> from(ThrowingSupplier<T, ? extends Throwable> f) {
        try {
            return Result.ok(f.get());
        } catch (Throwable error) {
            return Result.error(error);
        }

    }

    @SuppressWarnings("unchecked")
    public static <T, E extends Throwable> Result<T, E> from(ThrowingSupplier<T, ? extends E> f, Class<E> errorType) {
        try {
            return Result.ok(f.get());
        } catch (Throwable error) {
            // The only check exception that f can throw is an exception of type E.
            // "All Throwables except subclasses of java.lang.RuntimeException or
            // java.lang.Error are checked."
            // Therefore, e is of type E, Error or RuntimeException

            if (errorType.isInstance(error))
                return Result.error((E) error);
            else if (error instanceof Error)
                throw (Error) error;
            else
                throw (RuntimeException) error;
        }
    }

    /*
     * public <U> Result<U, E> map(Function<T, U> op) { return isOk() ?
     * Result.ok(op.apply(ok)) : Result.error(error); }
     * 
     * public <U> U mapOr(U fallback, Function<T, U> f) { return isOk() ?
     * f.apply(ok) : fallback; }
     * 
     * 
     * 
     * public <U> Result<U, E> and(Result<U, E> res) { return this.isError() ?
     * Result.error(error) : res; }
     * 
     * public <U> Result<U, E> andThen(Supplier<Result<U, E>> op) { return
     * this.isError() ? Result.error(error) : op.get(); }
     */

    public T unwrap() {
        if (isOk())
            return ok;
        else
            throw new IllegalStateException("Called Result#unwrap() on an `Error` value", error);
    }

    public E unwrapError() {
        if (isError())
            return error;
        else
            throw new IllegalStateException("Called Result#unwrapError() on an `Ok` value");
    }

    @SuppressWarnings("unchecked")
    public <S extends E> S unwrapError(Class<S> clazz) {
        if (isError()) {
            if (clazz.isInstance(error))
                return (S) error;
            else
                throw new IllegalStateException(
                        "Called Result#unwrapError(Class<S>) on an `Error` value that was not of class S", error);
        } else
            throw new IllegalStateException("Called Result#unwrapError(Class<S>) on an `Ok` value");
    }
}
