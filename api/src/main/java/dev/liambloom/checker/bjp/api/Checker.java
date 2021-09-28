package dev.liambloom.checker.bjp.api;

import dev.liambloom.checker.bjp.Chapter;
import dev.liambloom.checker.bjp.Exercise;
import dev.liambloom.checker.bjp.ProgrammingProject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Checker {
    private Checker() {
    }

    private static final ResourcePool<XPath> xPathPool = new ResourcePool<>(XPathFactory.newInstance()::newXPath);

    static ResourcePool<XPath> getXPathPool() {
        return xPathPool;
    }

    public static Stream<Result<TestStatus>> check(CheckArgs args) throws XPathExpressionException {
        List<Class<?>> classes;
        int chapter;
        try {
            AtomicInteger detectedChapter = new AtomicInteger(-1);
            classes = new PathClassLoader(args.paths()).loadAllOwnClasses()
                .filter(clazz -> {
                    Chapter ch = clazz.getAnnotation(Chapter.class);
                    if (ch == null)
                        return false;
                    else if (args.chapter().isPresent()) {
                        return ch.value() == args.chapter().getAsInt();
                    }
                    else if (detectedChapter.compareAndExchange(-1, ch.value()) != ch.value())
                        throw new UserErrorException("Cannot auto detect chapter, as classes belonging to chapters " + ch.value() + " and " + detectedChapter + " were found");
                    System.out.println(ch);
                    return true;
                })
                .collect(Collectors.toList());
            System.out.println();
            chapter = args.chapter().orElseGet(detectedChapter::get);
        }
        catch (LinkageError | IOException e) {
            throw new UserErrorException(e);
        }

        XPath xpath1 = xPathPool.get();
        Node ch = (Node) xpath1.evaluate("/book/chapter[@num='" + chapter + "']", args.tests(), XPathConstants.NODE);
        xPathPool.offer(xpath1);


        return Stream.of(new TargetGroup(Exercise.class, args.exercises()), new TargetGroup(ProgrammingProject.class, args.programmingProjects()))
            .map(e -> e.addPotentialTargets(classes.iterator()))
            .flatMap(e -> e.apply(ch))
            .map(Test::run);
    }

    private static class TargetGroup {
        private final Targets[] targets;
        private final Class<? extends Annotation> annotationType;
        private final Function<Annotation, Integer> valueOf;

        public TargetGroup(Class<? extends Annotation> annotationType, boolean[] initWhich) {
            targets = new Targets[initWhich.length];
            for (int i = 0; i < initWhich.length; i++) {
                if (initWhich[i])
                    targets[i] = new Targets();
            }
            Method m;
            try {
                m = annotationType.getMethod("value");
            }
            catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }

            valueOf = a -> {
                try {
                    return (Integer) m.invoke(a);
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalArgumentException(e);
                }
            };

            this.annotationType = annotationType;
        }

        public void addPotentialTarget(AnnotatedElement e) {
            Optional.ofNullable(targets[valueOf.apply(e.getAnnotation(annotationType))])
                .ifPresent(t -> t.add(e));
        }

        public TargetGroup addPotentialTargets(Iterator<? extends AnnotatedElement> iter) {
            iter.forEachRemaining(this::addPotentialTarget);
            return this;
        }

        public Stream<Test> apply(Node tests) {
            Stream.Builder<Test> builder = Stream.builder();
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null) {
                    String testName = Case.convert(annotationType.getSimpleName(), Case.TITLE) + ' ' + i;

                    if (targets[i].isEmpty()) {
                        builder.add(Test.withFixedResult(new Result<>(testName, TestStatus.INCOMPLETE)));
                    }
                    else {
                        XPath xpath = null;
                        try {
                            builder.add(Test.multiTest(testName, targets[i],
                                Optional.ofNullable(
                                    (xpath = Checker.getXPathPool().get())
                                        .evaluate(Case.convert(annotationType.getSimpleName(), Case.CAMEL) + "[@num='" + i + "']", tests, XPathConstants.NODE))
                                    .map(Element.class::cast)
                                    .orElseThrow(() -> new UserErrorException("Unable to find tests for " + testName))));
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
            }
            return builder.build();
        }
    }
}
