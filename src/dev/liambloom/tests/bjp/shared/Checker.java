package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.Chapter;
import dev.liambloom.tests.bjp.Exercise;
import dev.liambloom.tests.bjp.ProgrammingProject;
import org.w3c.dom.Document;
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
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Checker {
    private Checker() {}

    private static final ResourcePool<XPath> xPathPool = new ResourcePool<>(XPathFactory.newInstance()::newXPath);

    static ResourcePool<XPath> getXPathPool() {
        return xPathPool;
    }

    public static Stream<Result> check(CheckArgs args)
            throws IOException, XPathExpressionException {
        List<Class<?>> classes;
        int chapter;
        try {
            AtomicInteger detectedChapter = new AtomicInteger(-1);
            classes = new PathClassLoader(args.paths()).loadAllClasses()
                    .filter(clazz -> {
                        Chapter ch = clazz.getAnnotation(Chapter.class);
                        if (ch == null)
                            return false;
                        else if (args.chapter().isPresent()) {
                            return ch.value() == args.chapter().getAsInt();
                        }
                        else if (detectedChapter.updateAndGet(c -> c == -1 ? ch.value() : c) != ch.value())
                            throw new UserErrorException("Cannot auto detect chapter, as classes belonging to chapters " + ch.value() + " and " + detectedChapter + " were found");
                        return true;
                    })
                    .collect(Collectors.toList());
            chapter = args.chapter().orElseGet(detectedChapter::get);
        }
        catch (LinkageError e) {
            throw new UserErrorException(e);
        }

        XPath xpath1 = xPathPool.get();
        Node ch = (Node) xpath1.evaluate("/book/chapter[@num='" + chapter  + "']", args.tests(), XPathConstants.NODE);
        xPathPool.offer(xpath1);


        try {
            return Stream.of(new TargetGroup(Exercise.class, args.exercises()), new TargetGroup(ProgrammingProject.class, args.programmingProjects()))
                    .map(e -> e.addPotentialTargets(classes.iterator()))
                    .flatMap(TargetGroup::tests)
                    .map(d -> d.newTest(ch))
                    .map(Test::run);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class TargetGroup {
        private final Targets[] targets;
        private final Class<? extends Annotation> annotationType;
        private final Function<Annotation, Integer> valueOf;

        public TargetGroup(Class<? extends Annotation> annotationType, boolean[] initWhich) throws NoSuchMethodException {
            targets = new Targets[initWhich.length];
            for (int i = 0; i < initWhich.length; i++) {
                if (initWhich[i])
                    targets[i] = (Targets) Collections.synchronizedSet(new Targets());
            }

            Method m = annotationType.getMethod("value");
            valueOf = a -> {
                try {
                    return (Integer) m.invoke(a);
                }
                catch (IllegalAccessException | InvocationTargetException e) { throw new IllegalArgumentException(e); }
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

        public Stream<Test.ReflectionData> tests() {
            Stream.Builder<Test.ReflectionData> builder = Stream.builder();
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null)
                    builder.add(new Test.ReflectionData(targets[i], annotationType, i + 1));
            }
            return builder.build();
        }
    }
}
