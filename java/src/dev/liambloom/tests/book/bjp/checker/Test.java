package dev.liambloom.tests.book.bjp.checker;

import org.w3c.dom.Element;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.regex.Pattern;

public class Test {
    public static final Class<?> INACCESSIBLE_OBJECT_EXCEPTION_CLASS;

    public static class Target {
        private final List<AnnotatedElement> targets;
        private final Class<? extends Annotation> annotationType;
        private final int num;

        public Target(List<AnnotatedElement> targets, Class<? extends Annotation> annotationType, int num) {
            this.targets = targets;
            this.annotationType = annotationType;
            this.num = num;
        }
    }

    static {
        Class<?> temp;
        try {
            temp = Test.class.getClassLoader().loadClass("java.lang.reflect.InaccessibleObjectException");
        }
        catch (ClassNotFoundException e) {
            temp = Void.class;
        }
        INACCESSIBLE_OBJECT_EXCEPTION_CLASS = temp;
    }

    protected boolean type;
    protected int num;
    protected InputStream sysIn = new InputStream() { // Or from xml
        @Override
        public int read() throws IOException {
            return -1;
        }
    };
    protected Object[] args = new Object[0]; // TODO
    protected String prints = null;
    protected Element returns = null;
    protected Class<? extends Throwable> throwsErr = null;
    //protected Map<Matcher, Object> pre;
    //protected Map<Matcher, Object> post;

    //private Test() {}

    public Test()

    /*public final class Builder {
        private Test test = new Test();

        public Test build() {
            if (test == null)
                throw new IllegalStateException();
            try {
                return test;
            }
            finally {
                test = null;
            }
        }

        public Builder prints(String s) {
            if (test.prints != null)

            test.prints = s;
            return this;
        }

        public Builder returns(Object o) {

        }
    }*/

    public static Test c1e1() {
        Test t = new Test();
        t.prints = "//////////////////////\n" +
                "|| Victory is mine! ||\n" +
                "\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\";
        t.type = TestResult.EX;
        t.num = 1;
        return t;
    }

    public TestResult test(Method m) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream outWrapper = new PrintStream(out);
        InputStream defaultIn = System.in;
        PrintStream defaultOut = System.out;
        PrintStream defaultErr = System.err;
        System.setIn(sysIn);
        System.setOut(outWrapper);
        System.setErr(outWrapper);
        Object target;
        if (Modifier.isStatic(m.getModifiers())) // TODO: check if "this" is defined
            target = null;
        else {
            // TODO
            target = null;
        }
        // m.setAccessible() throws an InaccessibleObjectException in java 9+. But that exception is only defined in java 9+.
        try {
            m.setAccessible(true);
        }
        catch (RuntimeException e) {
            if (INACCESSIBLE_OBJECT_EXCEPTION_CLASS.isInstance(e)) {}
                //TODO: return new TestResult(this.type, this.num, ); // Method inaccessible
            else
                throw e;
        }
        try {
            Object r = m.invoke(target, args);

            if (!normalizeLineSeparators(out.toString()).equals(normalizeLineSeparators(prints))) {}
                // TODO: return new TestResult(this.type, this.num, TestResult.Variant.INCORRECT, );
        }
        catch (Throwable e) {
            if (throwsErr == null || !throwsErr.isInstance(e))
                return new TestResult(this.type, this.num, TestResult.Variant.INCORRECT, e, out);
            else
                return new TestResult(this.type, this.num, TestResult.Variant.CORRECT);
        }
        finally {
            System.setIn(defaultIn);
            System.setOut(defaultOut);
            System.setErr(defaultErr);
        }
        return null;
    }

    private static final Pattern LINE_SEPARATOR = Pattern.compile("\\r|\\r?\\n");

    private static String normalizeLineSeparators(String s) {
        return LINE_SEPARATOR.matcher(s).replaceAll(System.lineSeparator());
    }

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
