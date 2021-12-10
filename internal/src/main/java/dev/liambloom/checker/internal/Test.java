package dev.liambloom.checker.internal;

import dev.liambloom.checker.NotYetImplementedError;
import dev.liambloom.checker.ReLogger;
import dev.liambloom.checker.Result;
import dev.liambloom.checker.TestStatus;
import dev.liambloom.util.StringUtils;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Test {
    Future<Result<TestStatus>> start();

    ReadWriteLock testLock = new ReentrantReadWriteLock();
    ExecutorService readOnlyTest = Executors.newCachedThreadPool();
    ExecutorService writingTest = Executors.newSingleThreadExecutor();

    static Test withFixedResult(Result<TestStatus> result) {
        return () -> CompletableFuture.completedFuture(result);
    }

    static Test multiTest(String name, Targets targets, Node testGroup) {
        Stream<Test> subTests = Util.streamNodeList(testGroup.getChildNodes())
            .map(Element.class::cast)
            .flatMap(node -> {
                switch (node.getTagName()) {
                    case "method" -> {
                        if (targets.methods().isEmpty())
                            return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.INCOMPLETE)));
                        else if (targets.methods().size() == 1) {
                            Method method = targets.methods().iterator().next();
                            if (!Modifier.isStatic(method.getModifiers())) {
                                ReLogger logger = new ReLogger(Test.class.getName());
                                logger.log(System.Logger.Level.ERROR, "Bad Header: Instance method %s should be static", Util.executableToString(method));
                                return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.BAD_HEADER, logger)));
                            }
                            return Test.streamFromStaticExecutable(name, method, p -> method.invoke(null, p), targets, testGroup);
                        }
                        else {
                            throw new NotYetImplementedError("Resolving method from multiple target");
                        }
                    }
                    case "constructor" -> {
                        if (targets.constructors().isEmpty())
                            return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.INCOMPLETE)));
                        else if (targets.constructors().size() == 1) {
                            Constructor<?> c = targets.constructors().iterator().next();
                            return Test.streamFromStaticExecutable(name, c, c::newInstance, targets, testGroup);
                        }
                        else {
                            throw new NotYetImplementedError("Resolving constructor from multiple target");
                        }
                    }
                    case "project" -> {
                        throw new NotYetImplementedError("Resolving project");
                    }
                    default -> throw new IllegalStateException("This should not have passed the schema");
                }
            });
        return () -> readOnlyTest.submit(() -> {
            List<Result<TestStatus>> subResults = subTests.sequential()
                .map(Test::start)
                .map(FunctionUtils.unchecked((FunctionThrowsException<Future<Result<TestStatus>>, Result<TestStatus>>) Future::get))
                .collect(Collectors.toList());
            return new Result<>(
                name,
                subResults.stream()
                    .map(Result::status)
                    .max(Comparator.naturalOrder())
                    .orElseThrow(),
                subResults);
        });
    }

    static Stream<Test> streamFromStaticExecutable(String name, Executable executable, ExecutableInvocation invoke, Targets targets, Node node) {
        if (!executable.canAccess(null) && !executable.trySetAccessible()) {
            ReLogger logger = new ReLogger(Test.class.getName());
            logger.log(System.Logger.Level.ERROR, "Bad Header: %s %s %s is not accessible",
                StringUtils.convertCase(Util.getAccessibilityModifierName(executable), StringUtils.Case.SENTENCE),
                executable.getClass().getSimpleName().toLowerCase(Locale.ENGLISH),
                Util.executableToString(executable));
            return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.BAD_HEADER, logger)));
        }
        XPath xpath = Util.getXPathPool().get();
        NodeList expectedParamNodes;
        try {
            expectedParamNodes = (NodeList) xpath.evaluate("parameters/parameter", node, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        finally {
            Util.getXPathPool().offer(xpath);
        }
        Class<?>[] params = MethodType.methodType(void.class, executable.getParameterTypes()).wrap().parameterArray();
        Class<?>[] expectedParams = MethodType.methodType(void.class, Util.streamNodeList(expectedParamNodes)
            .map(Node::getTextContent)
            .map(String::trim)
            .map(n -> {
                try {
                    return Util.loadClass(new ClassLoader() {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                            if (name.equals("this"))
                                return executable.getDeclaringClass();
                            else
                                throw new ClassNotFoundException(name);
                        }
                    }, n);
                }
                catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Invalid document passed into Test.streamFromStaticExecutable", e);
                }
            })
            .collect(Collectors.toList())
        )
            .wrap()
            .parameterArray();
        boolean isCorrectParams = false;
        boolean usesVarArgs = false;
        if (params.length == expectedParams.length || executable.isVarArgs() && expectedParams.length >= params.length - 1) {
            for (int i = 0; i < expectedParams.length; i++) {
                if (!(isCorrectParams = i < params.length && params[i].isAssignableFrom(expectedParams[i])
                    && (!executable.isVarArgs() || i != params.length - 1 || params.length == expectedParams.length)
                    || (usesVarArgs = executable.isVarArgs() && i >= params.length - 1 && params[params.length - 1].getComponentType().isAssignableFrom(expectedParams[i]))))
                    break;
            }
        }

        if (isCorrectParams) {
            NodeList tests;
            try {
                tests = (NodeList) xpath.evaluate("test", node, XPathConstants.NODESET);
            }
            catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
            ExecutableInvocation invokeWrapper;
            if (usesVarArgs) {
                final int nonVarArgCount = params.length - 1;
                invokeWrapper = args -> {
                    Object[] newArgs = Arrays.copyOfRange(args, 0, nonVarArgCount + 1);
                    newArgs[nonVarArgCount] = new Object[args.length - nonVarArgCount];
                    System.arraycopy(args, nonVarArgCount + 1, newArgs[nonVarArgCount], 0, args.length - nonVarArgCount);
                    return invoke.invoke(newArgs);
                };
            }
            else
                invokeWrapper = invoke;
            return IntStream.range(0, tests.getLength())
                .mapToObj(i -> new StaticExecutableTest("Test " + i, executable, invokeWrapper, targets, tests.item(i)));
        }
        else {
            ReLogger logger = new ReLogger(Util.generateLoggerName());
            logger.log(System.Logger.Level.INFO, "%s %s was detected, but did not have the expected parameters (%s)",
                executable.getClass().getSimpleName(),
                Util.executableToString(executable),
                Util.streamNodeList(expectedParamNodes)
                    .map(Node::getTextContent)
                    .collect(Collectors.joining(", ")));
            return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.INCOMPLETE, logger)));
        }
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
