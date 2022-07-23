package dev.liambloom.checker.internal;

import dev.liambloom.checker.TestStatus;
import dev.liambloom.checker.books.*;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class CheckerTargetGroup<T extends Annotation> {
    private final Targets[] targets;
    private final CheckableType<T> checkableType;

    public CheckerTargetGroup(CheckableType<T> checkableType, boolean[] which) {
        targets = new Targets[which.length];
        for (int i = 0; i < which.length; i++) {
            if (which[i])
                targets[i] = new Targets();
        }

        this.checkableType = checkableType;
    }

    @SuppressWarnings("RedundantThrows")
    public void addPotentialTarget(AnnotatedElement e) throws InvocationTargetException {
        Optional.ofNullable(e.getAnnotation(checkableType.annotation()))
            .map(FunctionUtils.unchecked((FunctionThrowsException<T, Integer>)
                checkableType::value))
            .filter(i -> i < targets.length)
            .map(i -> targets[i])
            .ifPresent(t -> t.add(e));
    }

    public Stream<Test> apply(Chapter ch) {
        Stream.Builder<Test> builder = Stream.builder();
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] == null)
                continue;

            String testName = checkableType.name() + ' ' + i;

            StaticExecutableTest.Factory factory = new StaticExecutableTest.Factory(targets[i]);
            Test test;
            try {
                test = Test.multi(testName,
                    Arrays.stream(ch.getCheckable(checkableType, i).tests()).map(factory::newInstance));
                if (targets[i].isEmpty()) {
                    System.getLogger(System.identityHashCode(this) + "").log(System.Logger.Level.TRACE, "Checker target group %s has no targets", testName);
                    test = Test.of(testName, TestStatus.INCOMPLETE);
                }
            }
            catch (NullPointerException e) {
                test = Test.of(testName, TestStatus.NO_SUCH_TEST);
            }
            builder.add(test);
        }

        return builder.build();
    }
}
