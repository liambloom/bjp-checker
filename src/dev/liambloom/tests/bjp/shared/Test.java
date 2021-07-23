package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class Test {
    private static final Pattern LOOKAHEAD_CAPITAL = Pattern.compile("(?=(?<!^)[A-Z])");

    public static class Target {
        private final List<AnnotatedElement> targets;
        private final Class<? extends Annotation> annotationType;
        private final int num;

        public Target(List<AnnotatedElement> targets, Class<? extends Annotation> annotationType, int num) {
            this.targets = targets;
            this.annotationType = annotationType;
            this.num = num;
        }

        public Test getTest(Node tests, XPath xpath) {
            char[] testType = annotationType.getSimpleName().toCharArray();
            testType[0] = Character.toLowerCase(testType[0]);
            try {
                return new Test(targets,
                        (Node) Optional.ofNullable(xpath.evaluate(new String(testType) + "[@num='" + num + "']", tests, XPathConstants.NODE))
                                .orElseThrow(() -> new UserErrorException("Unable to find tests for " + toSpacedCase(annotationType.getSimpleName()) + num)));
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }
    }

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

    public Test(List<AnnotatedElement> targets, Node tests) {

    }

    public TestResult run() {
        return null; // TODO
    }

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
        try {
            m.setAccessible(true);
        }
        catch (InaccessibleObjectException e) {
            //TODO: return new TestResult(this.type, this.num, ); // Method inaccessible
        }
        try {
            Object r = m.invoke(target, args);

            if (!normalizeLineSeparators(out.toString()).equals(normalizeLineSeparators(prints))) {}
                // TODO: return new TestResult(this.type, this.num, TestResult.Variant.INCORRECT, );
        }
        catch (Throwable e) {
            if (throwsErr == null || !throwsErr.isInstance(e))
                return null; // TODO new TestResult(this.type, this.num, TestResult.Variant.INCORRECT, e, out);
            else
                return null;// TODO new TestResult(this.type, this.num, TestResult.Variant.CORRECT);
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

    public static String toSpacedCase(String s) {
        return LOOKAHEAD_CAPITAL.matcher(s).replaceAll(" ").toLowerCase();
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
