package dev.liambloom.checker.internal;

import org.w3c.dom.Element;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public record CheckableType<T extends Annotation>(String name, Class<T> annotation) {

    /**
     * Constructs a {@code CheckableType} from an {@code Element}
     *
     * @param e The element to construct from
     * @throws ClassNotFoundException If the class linked in the element could not be found.
     * @throws ClassCastException If the class linked in the element is not an annotation, or
     *          if the {@code value()} method does not return an int.
     * @throws NoSuchMethodException If the annotation does not have a {@code value()} method.
     */
    public CheckableType(Element e) throws ClassNotFoundException, NoSuchMethodException {
        this(e.getAttribute("name"), (Class<T>) ClassLoader.getSystemClassLoader().loadClass(e.getAttribute("annotation")));
        if (!Arrays.asList(annotation.getInterfaces()).contains(Annotation.class))
            throw new ClassCastException("Cannot convert " + annotation.getName() + " to java.lang.Annotation");
        Method m = annotation.getMethod("value");
        if (!m.getReturnType().equals(int.class))
            throw new ClassNotFoundException("Cannot convert " + m.getGenericReturnType() + " to int");
    }

    public int value(T target) throws InvocationTargetException, IllegalAccessException {
        Method m;
        try {
            m = annotation.getMethod("value");
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("This should never be thrown", e);
        }
        m.trySetAccessible();
        return (int) m.invoke(target);
    }
}
