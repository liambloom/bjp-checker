package dev.liambloom.checker.internal;

import dev.liambloom.checker.ReLogger;
import dev.liambloom.checker.Result;
import dev.liambloom.checker.TestStatus;
import dev.liambloom.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.invoke.MethodType;
import java.lang.reflect.Executable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StaticExecutableTest implements Test {
    private final String name;
//    private final Executable executable;
    private final ExecutableInvocation invoke;
//    private final Targets targets;
//    private final Node test;
    private final InputStream in;
    private final String expectedOut;
    private final ExecutorService executor;
    private final PrePost[] argConditions;
    private final Post expectedReturns;
    private final Class<? extends Throwable> expectedThrows;
    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    private final Lock lock;

    public StaticExecutableTest(String name, ExecutableInvocation invoke, Targets targets, Node test) {
        this.name = name;
//        this.executable = executable;
        this.invoke = invoke;
//        this.targets = targets;
//        this.test = test;
        int i = 0;
        List<Element> children = Util.streamNodeList(test.getChildNodes())
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .collect(Collectors.toList());
        in = children.get(i).getTagName().equals("System.in")
            ? new ByteArrayInputStream(children.get(i++).getTextContent().getBytes())
            : null;
        if (children.get(i).getTagName().equals("this")) // TODO: Update Schema
            throw new IllegalArgumentException("Element <this> invalid in top level method");
        argConditions = children.get(i).getTagName().equals("arguments")
            ? Util.streamNodeList(children.get(i++).getChildNodes())
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .map(PrePost::new)
            .toArray(PrePost[]::new)
            : new PrePost[0];
        Map<Path, String> writesTo;
        if (children.get(i).getTagName().equals("throws")) {
            try {
                //noinspection unchecked,UnusedAssignment
                expectedThrows = (Class<? extends Throwable>) ClassLoader.getSystemClassLoader().loadClass(children.get(i++).getTextContent());
            }
            catch (ClassNotFoundException | ClassCastException e) {
                throw new IllegalStateException("This should not have passed validation.", e);
            }
            expectedReturns = null;
            expectedOut = null;
            writesTo = Collections.emptyMap();
        }
        else {
            expectedThrows = null;
            expectedReturns = Optional.ofNullable(children.get(i))
                .filter(n -> n.getTagName().equals("returns"))
                .map(Post::new)
                .orElse(null);
            if (expectedReturns != null)
                i++;
            String rawExpectedPrints = Optional.ofNullable(children.get(i))
                .filter(n -> n.getTagName().equals("prints"))
                .map(Element::getTextContent)
                .map(String::stripIndent)
                .orElse(null);
            if (rawExpectedPrints == null)
                expectedOut = null;
            else {
                expectedOut = Util.cleansePrint(rawExpectedPrints);
                //noinspection UnusedAssignment
                i++;
            }
            writesTo = children.stream()
                .collect(Collectors.toMap(
                    e -> Path.of(e.getAttribute("href")),
                    e -> e.getTextContent().stripIndent()
                ));
        }
        if (in == null && expectedOut == null && writesTo.isEmpty()) {
            executor = readOnlyTest;
            lock = testLock.readLock();
        }
        else {
            executor = writingTest;
            lock = testLock.writeLock();
        }
        // TODO: Re-load classes to allow them to write to files in writesTo
    }

    public static Stream<Test> stream(String name, Executable executable, ExecutableInvocation invoke, Targets targets, Node node) {
        System.Logger logger = System.getLogger(Util.generateLoggerName());
        logger.log(System.Logger.Level.TRACE, "Streaming static executable tests for " + name);
        if (!executable.canAccess(null) && !executable.trySetAccessible()) {
            ReLogger resultDetails = new ReLogger(Test.class.getName());
            resultDetails.log(System.Logger.Level.ERROR, "Bad Header: %s %s %s is not accessible",
                StringUtils.convertCase(Util.getAccessibilityModifierName(executable), StringUtils.Case.SENTENCE),
                executable.getClass().getSimpleName().toLowerCase(Locale.ENGLISH),
                Util.executableToString(executable));
            return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.BAD_HEADER, resultDetails)));
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
        boolean isCorrectParams = true;
        boolean usesVarArgs = false;
        if (params.length == expectedParams.length || executable.isVarArgs() && expectedParams.length >= params.length - 1) {
            for (int i = 0; i < expectedParams.length; i++) {
                if (!(isCorrectParams = i < params.length && params[i].isAssignableFrom(expectedParams[i])
                    && (!executable.isVarArgs() || i != params.length - 1 || params.length == expectedParams.length)
                    || (usesVarArgs = executable.isVarArgs() && i >= params.length - 1 && params[params.length - 1].getComponentType().isAssignableFrom(expectedParams[i]))))
                    break;
            }
        }

        logger.log(System.Logger.Level.TRACE, "Static Executable %s has correct params: %b", name, isCorrectParams);

        if (isCorrectParams) {
            NodeList tests;
            try {
                tests = (NodeList) xpath.evaluate("test", node, XPathConstants.NODESET);
            }
            catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
            logger.log(System.Logger.Level.TRACE, "There are %d tests for static executable %s", tests.getLength(), name);
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

            Stream.Builder<Test> builder = Stream.builder();
            int testNumber = 1;
            for (int i = 0; i < tests.getLength(); i++) {
                Node n = tests.item(i);
                if (n instanceof Element e)
                    builder.add(new StaticExecutableTest("Test " + testNumber++, invokeWrapper, targets, e));
            }
            return builder.build();
        }
        else {
            ReLogger resultDetails = new ReLogger(Util.generateLoggerName());
            resultDetails.log(System.Logger.Level.INFO, "%s %s was detected, but did not have the expected parameters (%s)",
                executable.getClass().getSimpleName(),
                Util.executableToString(executable),
                Util.streamNodeList(expectedParamNodes)
                    .map(Node::getTextContent)
                    .collect(Collectors.joining(", ")));
            return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.INCOMPLETE, resultDetails)));
        }
    }

    @Override
    public Future<Result<TestStatus>> start() {
        if (hasRun.getAndSet(true))
            throw new IllegalStateException("Test has already been run");
        return executor.submit(() -> {
            lock.lock();
            try {
                System.setIn(in == null ? InputStream.nullInputStream(): in);
                ByteArrayOutputStream actualOut = expectedOut == null ? null : new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(expectedOut == null ? OutputStream.nullOutputStream() : actualOut);
                System.setOut(ps);
                System.setErr(ps);
                Object[] args = new Object[argConditions.length];
                for (int j = 0; j < argConditions.length; j++)
                    args[j] = argConditions[j].getPre();
                Object actualReturn;
                Throwable actualThrows;
                try {
                    actualReturn = invoke.invoke(args);
                    actualThrows = null;
                }
                catch (Throwable t) {
                    actualThrows = t;
                    actualReturn = null;
                }
                TestStatus status = TestStatus.OK;
                ReLogger logger = new ReLogger(Long.toString(System.identityHashCode(this)));
                boolean consoleInLogger;
                List<Result<? extends TestStatus>> subResults = new ArrayList<>();
                if (expectedThrows != null) {
                    if (!expectedThrows.isInstance(actualThrows)) {
                        logger.log(System.Logger.Level.ERROR, "Expected " + expectedThrows.getCanonicalName() + " to be thrown, but "
                            + (actualThrows == null ? "nothing" : actualThrows.getClass().getCanonicalName()) + " was thrown instead");
                    }
                }
                if (expectedReturns != null) {
                    Result<TestStatus> post = expectedReturns.check(actualReturn);
                    if (status.compareTo(post.status()) < 0)
                        status = post.status();
                    post.logs().ifPresent(l -> l.logTo(logger));
                    subResults.addAll(post.subResults());
                }
                String cleansedActualOutput;
                if (expectedOut != null && !expectedOut.equals(cleansedActualOutput = Util.cleansePrint(actualOut.toString()))) {
                    StringBuilder errMsg = new StringBuilder();
                    errMsg.append("Incorrect Console Output:\n")
                        .append("  Expected:");
                    for (String line : expectedOut.split("\\n")) {
                        errMsg.append("\n  | ")
                            .append(line);
                    }
                    errMsg.append("\n  Actual:");
                    for (String line : cleansedActualOutput.split("\\n")) {
                        errMsg.append("\n  | ")
                            .append(line);
                    }
                    logger.log(System.Logger.Level.ERROR, errMsg.toString());
                    status = TestStatus.FAILED;
                    consoleInLogger = true;
                }
                else
                    consoleInLogger = false;
                // TODO: DRY -- this code is repeated from the return value check
                for (int j = 0; j < args.length; j++) {
                    Result<TestStatus> post = argConditions[j].checkPost(args[j]);
                    if (status.compareTo(post.status()) < 0)
                        status = post.status();
                    post.logs().ifPresent(l -> l.logTo(logger));
                    subResults.addAll(post.subResults());
                }
                return new Result<>(name, status, Optional.of(logger), consoleInLogger || status != TestStatus.FAILED ? Optional.empty() : Optional.ofNullable(actualOut), subResults);
            }
            finally {
                lock.unlock();
            }
        });
    }
}
