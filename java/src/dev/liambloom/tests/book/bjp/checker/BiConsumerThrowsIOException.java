package dev.liambloom.tests.book.bjp.checker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface BiConsumerThrowsIOException<T, U> extends BiConsumer<T, U> {
    @Override
    default void accept(T t, U u) {
        try {
            acceptThrows(t, u);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void acceptThrows(T t, U u) throws IOException;
}