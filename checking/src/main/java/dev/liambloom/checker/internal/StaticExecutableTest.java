package dev.liambloom.checker.internal;

import dev.liambloom.checker.TestStatus;
import dev.liambloom.checker.books.*;
import dev.liambloom.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.lang.System.Logger.Level;

public class StaticExecutableTest implements Test {
    private static final Pattern TRAILING_SPACES_AND_NEWLINE = Pattern.compile("\\s*\\R");
    private final String name;
    private final ExecutableInvocation invoke;
    private final StaticExecutableTestInfo.Conditions conditions;
    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    private final ExecutorService executor;
    private final Lock lock;

    private StaticExecutableTest(String name, Executable executable, ExecutableInvocation invoke, StaticExecutableTestInfo.Conditions conditions) {
        this.name = name;
        this.invoke = invoke;
        this.conditions = conditions;
        if (conditions.in() == null && conditions.expectedOut() == null && conditions.writesTo().isEmpty()) {
            executor = Test.readOnlyTest;
            lock = Test.testLock.readLock();
        }
        else {
            executor = Test.writingTest;
            lock = Test.testLock.writeLock();
        }
    }

    public static class Factory {
        private static final Map<StaticExecutableTestInfo.TargetLocator, Function<StaticExecutableTestInfo.Conditions, Test>> locatedTargets = Collections.synchronizedMap(new HashMap<>());
        private final AtomicInteger counter = new AtomicInteger(1);
        private final Targets targets;

        Factory(Targets targets) {
            this.targets = targets;
        }

        private String getName() {
            return "Test " + counter.getAndIncrement();
        }

        public Test newInstance(StaticExecutableTestInfo info) {
            return newInstance(info.locator(), info.conditions());
        }

        public Test newInstance(StaticExecutableTestInfo.TargetLocator targetLocator, StaticExecutableTestInfo.Conditions conditions) {
//            String name = "Test " + counter.getAndIncrement();
            return locatedTargets.computeIfAbsent(targetLocator, __ -> {
                Set<? extends AnnotatedElement> filteredTargets = switch (targetLocator.type()) {
                    case METHOD -> targets.methods();
                    case CONSTRUCTOR -> targets.constructors();
                    case PROGRAM -> {
                        Set<AnnotatedElement> projectTargets = targets.classes().stream()
                            .filter(t -> {
                                try {
                                    Method main = t.getDeclaredMethod("main", String[].class);
                                    int mod = main.getModifiers();
                                    return Modifier.isPublic(mod) && Modifier.isStatic(mod) && main.getReturnType() == void.class;
                                }
                                catch (NoSuchMethodException e) {
                                    return false;
                                }
                            })
                            .collect(Collectors.toSet());
                        projectTargets.addAll(targets.methods());
                        yield projectTargets;
                    }
                };
                final Executable executable;
                final ExecutableInvocation invoke;
                if (filteredTargets.isEmpty()) {
                    System.getLogger(System.identityHashCode(this) + "")
                        .log(Level.TRACE, "All targets filtered out for static executable %s %s", targetLocator.type());
                    return _cond -> () -> CompletableFuture.completedFuture(new Result<>(getName(), TestStatus.INCOMPLETE));
                }
                else if (filteredTargets.size() == 1) {
                    AnnotatedElement target = filteredTargets.iterator().next();
                    if (target instanceof Class<?> c) {
                        try {
                            target = c.getDeclaredMethod("main", String[].class);
                        }
                        catch (NoSuchMethodException e) {
                            throw new IllegalStateException("Unreachable: Class should have been filtered out", e);
                        }
                    }
                    if (target instanceof Constructor<?> c) {
                        executable = c;
                        invoke = c::newInstance;
                    }
                    else if (target instanceof Method m) {
                        if (!Modifier.isStatic(m.getModifiers())) {
                            ReLogger logger = new ReLogger(Test.class.getName());
                            logger.log(System.Logger.Level.ERROR, "Bad Header: Instance method %s should be static", executableToString(m));
                            return _cond -> () -> CompletableFuture.completedFuture(new Result<>(getName(), TestStatus.BAD_HEADER, logger));
                        }
                        executable = m;
                        invoke = p -> m.invoke(null, p);
                    }
                    else
                        throw new IllegalStateException("Unreachable: Target of impossible type");
                }
                else
                    throw new Error("Resolving test target from multiple options has not been implemented");

                // TODO: Do checks from internal/StaticExecutableTest.stream to make sure executable is valid
                if (!executable.canAccess(null) && !executable.trySetAccessible()) {
                    ReLogger resultDetails = new ReLogger(Test.class.getName());
                    resultDetails.log(System.Logger.Level.ERROR, "Bad Header: %s %s %s is not accessible",
                        StringUtils.convertCase(getAccessibilityModifierName(executable), StringUtils.Case.SENTENCE),
                        executable.getClass().getSimpleName().toLowerCase(Locale.ENGLISH),
                        executableToString(executable));
                    return _cond -> () -> CompletableFuture.completedFuture(new Result<>(getName(), TestStatus.BAD_HEADER, resultDetails));
                }

                Class<?>[] params = MethodType
                    .methodType(void.class, executable.getParameterTypes())
                    .wrap()
                    .parameterArray();
                boolean isCorrectParams = false;
                boolean usesVarArgs = false;
                if (params.length == targetLocator.params().length || executable.isVarArgs() && targetLocator.params().length >= params.length - 1) {
                    isCorrectParams = true;
                    for (int i = 0; i < targetLocator.params().length; i++) {
                        if (!(isCorrectParams = i < params.length && params[i].isAssignableFrom(targetLocator.params()[i])
                            && (!executable.isVarArgs() || i != params.length - 1 || params.length == targetLocator.params().length)
                            || (usesVarArgs = executable.isVarArgs() && i >= params.length - 1 && params[params.length - 1].getComponentType().isAssignableFrom(targetLocator.params()[i]))))
                            break;
                    }
                }

                if (isCorrectParams) {
                    ExecutableInvocation invokeWrapper;
                    if (usesVarArgs) {
                        final int nonVarArgCount = params.length - 1;
                        invokeWrapper = args -> {
                            Object[] newArgs = Arrays.copyOfRange(args, 0, nonVarArgCount + 1);
                            newArgs[nonVarArgCount] = new Object[args.length - nonVarArgCount];
                            //noinspection SuspiciousSystemArraycopy
                            System.arraycopy(args, nonVarArgCount + 1, newArgs[nonVarArgCount], 0, args.length - nonVarArgCount);
                            return invoke.invoke(newArgs);
                        };
                    }
                    else
                        invokeWrapper = invoke;

                    return cond -> new StaticExecutableTest(getName(), executable, invokeWrapper, cond);
                }
                else {
                    ReLogger resultDetails = new ReLogger(this.getClass().getName() + targetLocator.hashCode());
                    resultDetails.log(System.Logger.Level.INFO, "%s %s was detected, but did not have the expected parameters (%s)",
                        executable.getClass().getSimpleName(),
                        executableToString(executable),
                        Arrays.stream(targetLocator.params())
                                .map(Class::getName)
                                .collect(Collectors.joining(", ")));
                    return _cond -> () -> CompletableFuture.completedFuture(new Result<>(getName(), TestStatus.INCOMPLETE, resultDetails));
                }
            }).apply(conditions);

//            return new StaticExecutableTest(name, executable, invoke, conditions);
        }

        private static String executableToString(Executable e) {
            StringBuilder builder = new StringBuilder()
                .append(e.getDeclaringClass().getName())
                .append('.')
                .append(e.getName())
                .append('(');
            java.lang.reflect.Type[] args = e.getGenericParameterTypes();
            for (int i = 0; i < args.length; i++) {
                if (i + 1 == args.length && e.isVarArgs()) {
                    builder.append(args[i].getTypeName())
                        .replace(builder.length() - 2, builder.length(), "...");
                }
                else
                    builder.append(args[i].getTypeName());
            }
            builder.append(')');
            return builder.toString();
        }

        private static String getAccessibilityModifierName(int modifier) {
            if (Modifier.isPublic(modifier))
                return "public";
            else if (Modifier.isProtected(modifier))
                return "protected";
            else if (Modifier.isPrivate(modifier))
                return "private";
            else
                return "package-private";
        }

        private static String getAccessibilityModifierName(Member m) {
            return getAccessibilityModifierName(m.getModifiers());
        }
    }

    @Override
    public Future<Result<TestStatus>> start() {
        if (hasRun.getAndSet(true))
            throw new IllegalStateException("Test has already been run");
        return executor.submit(() -> {
            lock.lock();
            try {
                try (InputStream in = conditions.in()) {
                    System.setIn(in == null ? InputStream.nullInputStream() : in);
                }
                ByteArrayOutputStream actualOut = conditions.expectedOut() == null ? null : new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(conditions.expectedOut() == null ? OutputStream.nullOutputStream() : actualOut);
                System.setOut(ps);
                System.setErr(ps);
                Object actualReturn;
                Throwable actualThrows;
                try {
                    actualReturn = invoke.invoke(conditions.args());
                    actualThrows = null;
                }
                catch (InvocationTargetException t) {
                    actualThrows = t.getCause();
                    actualReturn = null;
                }
                TestStatus status = TestStatus.OK;
                ReLogger logger = new ReLogger(Long.toString(System.identityHashCode(this)));
                boolean consoleInLogger;
                List<Result<? extends TestStatus>> subResults = new ArrayList<>();
                if (conditions.expectedThrows() != null) {
                    if (!conditions.expectedThrows().isInstance(actualThrows)) {
                        status = TestStatus.FAILED;
                        logger.log(System.Logger.Level.ERROR, "Expected " + conditions.expectedThrows().getCanonicalName() + " to be thrown, but "
                            + (actualThrows == null ? "nothing" : actualThrows.getClass().getCanonicalName()) + " was thrown instead");
                    }
                }
                else if (actualThrows != null) {
                    status = TestStatus.FAILED;
                    logger.log(System.Logger.Level.ERROR, "Unexpected error thrown", actualThrows);
                }
                if (conditions.expectedReturns() != null) {
                    Result<TestStatus> post = PostChecker.check(conditions.expectedReturns(), actualReturn);
                    if (status.compareTo(post.status()) < 0)
                        status = post.status();
                    post.logs().ifPresent(l -> l.logTo(logger));
                    subResults.addAll(post.subResults());
                }
                String cleansedExpectedOut = Optional.ofNullable(conditions.expectedOut()).map(StaticExecutableTest::cleansePrint).orElse(null);
                String cleansedActualOutput;
                if (conditions.expectedOut() != null && !cleansedExpectedOut.equals(cleansedActualOutput = cleansePrint(actualOut.toString()))) {
                    StringBuilder errMsg = new StringBuilder();
                    errMsg.append("Incorrect Console Output:\n")
                        .append("  Expected:");
                    for (String line : cleansedExpectedOut.split("\\n")) {
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
                for (int j = 0; j < conditions.args().length; j++) {
                    Result<TestStatus> post = PostChecker.check(conditions.argConditions()[j], conditions.args()[j]);
                    if (status.compareTo(post.status()) < 0)
                        status = post.status();
                    post.logs().ifPresent(l -> l.logTo(logger));
                    subResults.addAll(post.subResults());
                }
                return new Result<>(name, status, logger.isEmpty() ? Optional.empty() : Optional.of(logger), consoleInLogger || status != TestStatus.FAILED ? Optional.empty() : Optional.ofNullable(actualOut), subResults);
            }
            finally {
                lock.unlock();
            }
        });
    }

    static String cleansePrint(String raw) {
        String[] lines = TRAILING_SPACES_AND_NEWLINE.split(raw);
        if (lines.length == 0)
            return "";
        Stream<String> linesStream = Arrays.stream(lines);
        if (lines[lines.length - 1].isBlank())
            linesStream = linesStream.limit(lines.length - 1);
        if (lines[0].isEmpty())
            linesStream = linesStream.skip(1);
        return linesStream
            .map(String::stripTrailing)
            .collect(Collectors.joining(System.lineSeparator()))
            .replace('\u2013', '-')
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .replace('\u201B', '\'')
            .replace('\u201C', '"')
            .replace('\u201F', '"')
            .replace('\u201D', '"')
            .replace('\u301D', '"')
            .replace('\u301E', '"')
            .replace('\u201A', ',');
    }
}
