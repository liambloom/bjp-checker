package dev.liambloom.checker.bjp.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.UnaryOperator;


@FunctionalInterface
public interface UnaryOperatorThrowsIOException<T> extends UnaryOperator<T> {
    @Override
    default T apply(T t) {
        try {
            return applyThrows(t);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    T applyThrows(T t) throws IOException;
}