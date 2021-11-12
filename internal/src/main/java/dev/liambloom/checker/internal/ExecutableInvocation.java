package dev.liambloom.checker.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@FunctionalInterface
public interface ExecutableInvocation {
    Object invoke(Object[] args) throws InstantiationException, IllegalArgumentException, IllegalAccessException, InvocationTargetException;
}
