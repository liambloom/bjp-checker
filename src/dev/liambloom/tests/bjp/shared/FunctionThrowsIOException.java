package dev.liambloom.tests.bjp.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

@FunctionalInterface
public interface FunctionThrowsIOException<T, R> extends Function<T, R> {
    @Override
    default R apply(T t) {
        try {
            return applyThrows(t);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    R applyThrows(T t) throws IOException;
}
