package dev.liambloom.tests.bjp.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface BiFunctionThrowsIOException<T, U, R> extends BiFunction<T, U, R> {
    @Override
    default R apply(T t, U u) {
        try {
            return applyThrows(t, u);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    R applyThrows(T t, U u) throws IOException;
}
