package dev.liambloom.checker.books;

import dev.liambloom.util.StringUtils;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StaticExecutableTest implements Test {
    private final String name;
    //    private final Executable executable;
    private final ExecutableInvocation invoke;
    //    private final Targets targets;
//    private final Node test;
    private final Conditions conditions;
//    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    private final ExecutorService executor;
    private final Lock lock;

    private StaticExecutableTest(String name, Executable executable, ExecutableInvocation invoke, Conditions conditions) {
        this.name = name;
        this.invoke = invoke;
        this.conditions = conditions;
        if (conditions.in == null && conditions.expectedOut == null && conditions.writesTo.isEmpty()) {
            executor = readOnlyTest;
            lock = testLock.readLock();
        }
        else {
            executor = writingTest;
            lock = testLock.writeLock();
        }
    }

    public static class Factory {
        private static final Map<TargetLocator, Function<Conditions, Test>> locatedTargets = Collections.synchronizedMap(new HashMap<>());
        private final AtomicInteger counter = new AtomicInteger(1);
        private final Targets targets;

        Factory(Targets targets) {
            this.targets = targets;
        }

        private String getName() {
            return "Test " + counter.getAndIncrement();
        }

        public Test newInstance(TargetLocator targetLocator, Conditions conditions) {
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
                if (filteredTargets.isEmpty())
                    return _cond -> () -> CompletableFuture.completedFuture(new Result<>(getName(), TestStatus.INCOMPLETE));
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

    public enum Type {
        METHOD,
        CONSTRUCTOR,
        PROGRAM
    }

    public record Conditions(InputStream in,
                             Object[] args,
                             Post[] argConditions,
                             String expectedOut,
                             Post expectedReturns,
                             Class<? extends Throwable> expectedThrows,
                             Map<Path, String> writesTo) { }

    public record TargetLocator(Type type,
                                Optional<String> name,
                                Optional<Class<?>> container,
                                Class<?>[] params) { }
}
