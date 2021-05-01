package dev.liambloom.tests.book.bjp.checker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class Test {
    protected String prints;
    protected Object returns;
    protected Throwable error;
    protected Map<Matcher, Object> pre;
    protected Map<Matcher, Object> post;

    /*public TestResult test(Class<?> clazz) {
        for (Matcher matcher : pre.keySet()) {
            Field field = matcher.findField(clazz);
            field.setAccessible(true);
            field.set()
        }
        return null;
    }

    public TestResult test(Method method) {
        // TODO
        return null;
    }*/
}
