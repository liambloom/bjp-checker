package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface Test {
    Result run();

    record Target(List<AnnotatedElement> targets, Class<? extends Annotation> annotationType, int num) {
        public Test newTest(Node tests, XPath xpath) {
            String testName = Case.convert(annotationType().getSimpleName(), Case.SPACE) + ' ' + num;

            if (targets().isEmpty())
                return Test.withFixedResult(new Result(testName, TestStatus.INCOMPLETE));


            try {
                return Test.multiTest(Case.convert(annotationType.getSimpleName(), Case.CAMEL) + " " + num,targets,
                        Optional.ofNullable(xpath.evaluate(Case.convert(annotationType.getSimpleName(), Case.CAMEL) + "[@num='" + num + "']", tests, XPathConstants.NODE))
                                .map(Element.class::cast)
                                .orElseThrow(() -> new UserErrorException("Unable to find tests for " + testName)));
            } catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static Test withFixedResult(Result result) {
        return () -> result;
    }

    static Test multiTest(String name, List<AnnotatedElement> targets, Element tests) {
//        Stream.Builder<Test>
//        return () -> {
//            new ArrayList<Result>().stream()
//                    .map(Result::status)
//                    .map(TestStatus.class::cast)
//                    .max(Comparator.naturalOrder());
//            return new Result(name, )
//        };
        return null;
    }

    static Test methodTest(List<AnnotatedElement> targets, Node tests) {
        return null;
    }

//    static Test

    /*protected InputStream sysIn = new InputStream() { // Or from xml
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

    public Result run() {
        return null; // TODO
    }*/

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

    /*public Result test(Method m) {
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

    */



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
