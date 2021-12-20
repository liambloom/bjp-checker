package dev.liambloom.checker.internal;

import dev.liambloom.checker.NotYetImplementedError;
import dev.liambloom.checker.ReLogger;
import dev.liambloom.checker.Result;
import dev.liambloom.checker.TestStatus;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
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
                Set<? extends AnnotatedElement> filteredTargets = switch (node.getTagName()) {
                    case "method" -> targets.methods();
                    case "constructor" -> targets.constructors();
                    case "project" -> {
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
                    default -> throw new IllegalStateException("This should not have passed validation");
                };
                if (filteredTargets.isEmpty())
                    return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.INCOMPLETE)));
                else if (filteredTargets.size() == 1) {
                    AnnotatedElement target = filteredTargets.iterator().next();
                    if (target instanceof Constructor<?> c)
                        return StaticExecutableTest.stream(name, c, c::newInstance, targets, testGroup);
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
                        logger.log(System.Logger.Level.ERROR, "Bad Header: Instance method %s should be static", Util.executableToString(m));
                        return Stream.of(Test.withFixedResult(new Result<>(name, TestStatus.BAD_HEADER, logger)));
                    }
                    if (target instanceof Method m)
                        return StaticExecutableTest.stream(name, m, p -> m.invoke(null, p), targets, testGroup);

                    throw new IllegalStateException("Unreachable: Target of impossible type");
                }
                else
                    throw new NotYetImplementedError("Resolving test target from multiple options");
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
}
