package dev.liambloom.checker.internal;

import dev.liambloom.checker.ReLogger;
import dev.liambloom.checker.Result;
import dev.liambloom.checker.TestStatus;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.lang.reflect.Executable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public StaticExecutableTest(String name, Executable executable, ExecutableInvocation invoke, Targets targets, Node test) {
        this.name = name;
//        this.executable = executable;
        this.invoke = invoke;
//        this.targets = targets;
//        this.test = test;
        int i = 0;
        NodeList children = test.getChildNodes();
        in = ((Element) children.item(i)).getTagName().equals("System.in")
            ? new ByteArrayInputStream(children.item(i++).getTextContent().getBytes())
            : null;
        if (((Element) children.item(i)).getTagName().equals("this")) // TODO: Update Schema
            throw new IllegalArgumentException("Element <this> invalid in top level method");
        argConditions = ((Element) children.item(i)).getTagName().equals("arguments")
            ? Util.streamNodeList(children.item(i++).getChildNodes())
            .map(Element.class::cast)
            .map(PrePost::new)
            .toArray(PrePost[]::new)
            : new PrePost[0];
        Map<Path, String> writesTo;
        if (((Element) children.item(i)).getTagName().equals("throws")) {
            try {
                //noinspection unchecked
                expectedThrows = (Class<? extends Throwable>) ClassLoader.getSystemClassLoader().loadClass(children.item(i++).getTextContent());
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
            expectedReturns = Optional.ofNullable(children.item(i))
                .map(Element.class::cast)
                .filter(n -> n.getTagName().equals("returns"))
                .map(Post::new)
                .orElse(null);
            if (expectedReturns != null)
                i++;
            String rawExpectedPrints = Optional.ofNullable(children.item(i))
                .map(Element.class::cast)
                .filter(n -> n.getTagName().equals("prints"))
                .map(Element::getTextContent)
                .map(String::stripIndent)
                .orElse(null);
            if (rawExpectedPrints == null)
                expectedOut = null;
            else {
                expectedOut = Util.cleansePrint(rawExpectedPrints);
                i++;
            }
            writesTo = IntStream.range(i, children.getLength())
                .mapToObj(children::item)
                .map(Element.class::cast)
                .collect(Collectors.toMap(
                    e -> Path.of(e.getAttribute("href")),
                    e -> e.getTextContent().stripIndent()
                ));
        }
        executor = in == null && expectedOut == null && writesTo.isEmpty() ? readOnlyTest : writingTest;
        // TODO: Re-load classes to allow them to write to files in writesTo
    }

    @Override
    public Future<Result<TestStatus>> start() {
        return executor.submit(() -> {
            testLock.readLock().lock();
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
                testLock.writeLock().unlock();
            }
        });
    }
}
