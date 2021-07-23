package dev.liambloom.tests.bjp.gui;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

import java.util.function.Function;

public class MethodBinding<T, U> extends ObjectBinding<U> {
    private final ObservableValue<T> t;
    private final Function<T, U> f;

    public MethodBinding(ObservableValue<T> t, Function<T, U> f) {
        this.t = t;
        this.f = f;
        bind(t);
    }

    @Override
    public U computeValue() {
        return f.apply(t.getValue());
    }
}
