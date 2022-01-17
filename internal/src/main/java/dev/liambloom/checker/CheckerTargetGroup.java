package dev.liambloom.checker;

import dev.liambloom.checker.Result;
import dev.liambloom.checker.internal.Targets;
import dev.liambloom.checker.internal.Test;
import dev.liambloom.checker.internal.Util;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

class CheckerTargetGroup<T extends Annotation> {
    private final Targets[] targets;
    private final BookChecker.CheckableType<T> checkableType;

    public CheckerTargetGroup(BookChecker.CheckableType<T> checkableType, boolean[] initWhich) {
        targets = new Targets[initWhich.length];
        for (int i = 0; i < initWhich.length; i++) {
            if (initWhich[i])
                targets[i] = new Targets();
        }

        this.checkableType = checkableType;
    }

    @SuppressWarnings("RedundantThrows")
    public void addPotentialTarget(AnnotatedElement e) throws InvocationTargetException, IllegalAccessException {
        System.getLogger(Long.toString(System.identityHashCode(this))).log(System.Logger.Level.TRACE, "Potential target %s: Annotated with %s, looking for %s",
            e, Arrays.toString(e.getAnnotations()), checkableType.annotation()/* , targets[checkableType.value(e.getAnnotation(checkableType.annotation()))] == null ? "rejected" : "accepted" */);
        Optional.ofNullable(e.getAnnotation(checkableType.annotation()))
            .map(FunctionUtils.unchecked((FunctionThrowsException<T, Integer>) checkableType::value))
            .filter(i -> i < targets.length)
            .map(i -> targets[i])
            .ifPresent(t -> t.add(e));
    }

    @SuppressWarnings("RedundantThrows")
//    public void addPotentialTargets(Iterator<? extends AnnotatedElement> iter) throws InvocationTargetException, IllegalAccessException {
//        iter.forEachRemaining(FunctionUtils.unchecked(this::addPotentialTarget));
//    }

    public Stream<Test> apply(Node tests) {
        Stream.Builder<Test> builder = Stream.builder();
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] != null) {
                String testName = checkableType.name() + ' ' + i;

                System.getLogger(Long.toString(System.identityHashCode(this))).log(System.Logger.Level.TRACE, "Applying tests to %s with targets %s", testName, targets[i]);

                if (targets[i].isEmpty()) {
                    builder.add(Test.withFixedResult(new Result<>(testName, TestStatus.INCOMPLETE)));
                }
                else {
                    XPath xpath = null;
                    try {
                        builder.add(Test.multiTest(testName, targets[i],
                            Optional.ofNullable(
                                (xpath = Util.getXPathPool().get())
                                    .evaluate(checkableType.name() + "[@num='" + i + "']", tests, XPathConstants.NODE))
                                .filter(Element.class::isInstance)
                                .map(Element.class::cast)
                                .orElseThrow(() -> new IllegalArgumentException("Unable to find tests for " + testName))));
                    }
                    catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    }
                    finally {
                        if (xpath != null)
                            Util.getXPathPool().offer(xpath);
                    }
                }
            }
        }
        return builder.build();
    }
}
