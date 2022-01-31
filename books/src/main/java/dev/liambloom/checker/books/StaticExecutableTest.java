package dev.liambloom.checker.books;

import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        private final AtomicInteger counter = new AtomicInteger(1);
        private final Targets targets;

        Factory(Targets targets) {
            this.targets = targets;
        }

        public Test newInstance(Type type, Conditions conditions) {
            String name = "Test " + counter.getAndIncrement();
            Set<? extends AnnotatedElement> filteredTargets = switch (type) {
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
            Executable executable;
            ExecutableInvocation invoke;
            if (filteredTargets.isEmpty())
                return () -> CompletableFuture.completedFuture(new Result<>(name, TestStatus.INCOMPLETE));
            else if (filteredTargets.size() == 1) {
                AnnotatedElement target = filteredTargets.iterator().next();
                if (target instanceof Constructor<?> c) {
                    executable = c;
                    invoke = c::newInstance;
                }
                else if (target instanceof Class<?> c) {
                    try {
                        target = c.getDeclaredMethod("main", String[].class);
                    }
                    catch (NoSuchMethodException e) {
                        throw new IllegalStateException("Unreachable: Class should have been filtered out", e);
                    }
                }
                else if (target instanceof Method m && !Modifier.isStatic(m.getModifiers())) {
                    ReLogger logger = new ReLogger(Test.class.getName());
                    logger.log(System.Logger.Level.ERROR, "Bad Header: Instance method %s should be static", executableToString(m));
                    return () -> CompletableFuture.completedFuture(new Result<>(name, TestStatus.BAD_HEADER, logger));
                }
                if (target instanceof Method m) {
                    executable = m;
                    invoke = p -> m.invoke(null, p);
                }
                throw new IllegalStateException("Unreachable: Target of impossible type");
            }
            else
                throw new Error("Resolving test target from multiple options has not been implemented");

            // TODO: Do checks from internal/StaticExecutableTest.stream to make sure executable is valid
            return new StaticExecutableTest(name, executable, invoke, conditions);
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
                             Map<Path, String> writesTo) {
    }
}
