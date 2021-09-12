package dev.liambloom.checker.bjp.api;

import java.util.function.Function;
import java.util.function.Supplier;

// TODO: use this
public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> init;
    private T value = null;

    public Lazy(Supplier<T> initializer) {
        init = initializer;
    }

    @Override
    public synchronized T get() {
        if (value == null)
            value = init.get();
        return value;
    }

    public <U> Supplier<U> map(Function<T, U> f) {
        return () -> f.apply(get());
    }
}
