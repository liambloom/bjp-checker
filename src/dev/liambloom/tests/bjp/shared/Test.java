package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Test {
    Result run();

    record ReflectionData(Targets targets, Class<? extends Annotation> annotationType, int num) {
        public Test newTest(Node tests) {
            String testName = Case.convert(annotationType().getSimpleName(), Case.SPACE) + ' ' + num;

            if (targets().isEmpty())
                return Test.withFixedResult(new Result(testName, TestStatus.INCOMPLETE));


            XPath xpath = null;
            try {
                return Test.multiTest(Case.convert(annotationType.getSimpleName(), Case.CAMEL) + " " + num,targets,
                        Optional.ofNullable((xpath = Checker.getXPathPool().get()).evaluate(Case.convert(annotationType.getSimpleName(), Case.CAMEL) + "[@num='" + num + "']", tests, XPathConstants.NODE))
                                .map(Element.class::cast)
                                .orElseThrow(() -> new UserErrorException("Unable to find tests for " + testName)));
            }
            catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
            finally {
                if (xpath != null)
                    Checker.getXPathPool().offer(xpath);
            }
        }
    }

    static Test withFixedResult(Result result) {
        return () -> result;
    }

    static Test multiTest(String name, Targets targets, Node tests) {
        NodeList children = tests.getChildNodes();
        Stream<Test> subTests = IntStream.range(0, children.getLength())
                .parallel()
                .mapToObj(children::item)
                .map(Element.class::cast)
                .flatMap(node -> {
                    switch (node.getTagName()) {
                        case "method" -> {
                            if (targets.methods().isEmpty())
                                return Stream.of(Test.withFixedResult(new Result(name, TestStatus.INCOMPLETE)));
                            else if (targets.methods().size() == 1) {
                                Method method = targets.methods().iterator().next();
                                if (!Modifier.isStatic(method.getModifiers())) {
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    new BadHeaderException("Instance method " + TestUtils.executableToString(method) + " should be static").printStackTrace(new PrintStream(outputStream));
                                    return Stream.of(Test.withFixedResult(new Result(name, TestStatus.BAD_HEADER, Optional.of(outputStream))));
                                }
                                if (!method.canAccess(null) && !method.trySetAccessible()){
                                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                    new BadHeaderException(Case.convert(TestUtils.getAccessabilityModifierName(method), Case.SENTENCE)
                                            + " method "
                                            + TestUtils.executableToString(method)
                                            + " is not accessible")
                                            .printStackTrace(new PrintStream(outputStream));
                                    return Stream.of(Test.withFixedResult(new Result(name, TestStatus.BAD_HEADER, Optional.of(outputStream))));
                                }
                                XPath xpath = Checker.getXPathPool().get();
                                NodeList expectedParams;
                                try {
                                    expectedParams = (NodeList) xpath.evaluate("parameters/parameter", node, XPathConstants.NODESET);
                                }
                                catch (XPathExpressionException e) {
                                    throw new RuntimeException(e);
                                }
                                finally {
                                    Checker.getXPathPool().offer(xpath);
                                }
                                Class<?>[] params = method.getParameterTypes();
                                boolean isCorrectParams = true;
                                if (params.length != expectedParams.getLength() && !(method.isVarArgs() && expectedParams.getLength() >= params.length - 1))
                                    isCorrectParams = false;
                                else {
                                    for (int i = 0; i < expectedParams.getLength(); i++) {
                                        if (i >= params.length - 1 && method.isVarArgs()) {
                                            // TODO
                                        }
                                        else {
                                            try {
                                                if (!params[i].isAssignableFrom(Test.class.getClassLoader().loadClass(expectedParams.item(i).getTextContent().trim()))) {
                                                    isCorrectParams = false;
                                                    break;
                                                }
                                            } catch (ClassNotFoundException e) {
                                                throw new RuntimeException("This should have been caught earlier", e);
                                            }
                                        }
                                    }
                                }

                                if (isCorrectParams) {
                                    NodeList methodTests;
                                    try {
                                        methodTests = (NodeList) xpath.evaluate("test", node, XPathConstants.NODESET);
                                    } catch (XPathExpressionException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return IntStream.range(0, methodTests.getLength())
                                            .mapToObj(methodTests::item)
                                            .map(testNode -> Test.executableTest(method, targets, testNode));
                                }
                                else
                                    // TODO: Maybe add a outputstream that indicates to the user that there is a method
                                    //        of the correct name, but the parameters didn't match up.
                                    return Stream.of(Test.withFixedResult(new Result(name, TestStatus.INCOMPLETE)));
                                }
                            }
                            else {
                                // TODO
                            }
                        }
                        case "constructor" -> {
                            // TODO
                        }
                        case "project" -> {
                            // TODO
                        }
                        default -> throw new IllegalStateException("This should not have passed the schema");
                    }
                });
        return () -> {
            List<Result> subResults = subTests.map(Test::run).collect(Collectors.toList());
            return new Result(
                    name,
                    subResults.stream()
                            .map(Result::status)
                            .map(TestStatus.class::cast)
                            .max(Comparator.naturalOrder())
                            .get(),
                    Optional.empty(),
                    subResults);
        };
    }

    static Test executableTest(Executable executable, Targets targets, Node test) {
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
