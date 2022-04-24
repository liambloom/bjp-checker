package dev.liambloom.checker.books;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CheckableType<T extends Annotation> {
    private final String name;
    private final Class<T> annotation;
    private final Method value;

    public CheckableType(String name, Class<T> annotation) throws NoSuchMethodException, IllegalAccessException {
        this.name = name;
        this.annotation = annotation;
        value = annotation.getMethod("value");
        if (!value.getReturnType().equals(int.class))
            throw new ClassCastException("Cannot convert " + value.getGenericReturnType() + " to int");
        if (!value.trySetAccessible() && !Modifier.isPublic(value.getModifiers()))
            throw new IllegalAccessException(value + " is not accessible");
    }

    public String name() {
        return name;
    }

    public Class<T> annotation() {
        return annotation;
    }

    public int value(T target) throws InvocationTargetException {
        try {
            return (int) value.invoke(target);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("This should have thrown an exception in the constructor", e);
        }
    }

    public int valueGenetic(Annotation target) throws InvocationTargetException, ClassCastException {
        return value(annotation.cast(target));
    }

    @Override
    public String toString() {
        return "CheckableType " + name + ": " + annotation.getName();
    }
}
