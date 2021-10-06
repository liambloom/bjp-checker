package dev.liambloom.checker.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerThrowsIOException<T> extends Consumer<T> {
    @Override
    default void accept(T t) {
        try {
            acceptThrows(t);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void acceptThrows(T t) throws IOException;
}