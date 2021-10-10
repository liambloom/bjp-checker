package dev.liambloom.checker.internal;

import dev.liambloom.checker.shared.Result;
import dev.liambloom.checker.shared.ResourcePool;
import dev.liambloom.checker.shared.Util;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Checker {
    private Checker() {
    }

    private static final ResourcePool<XPath> xPathPool = new ResourcePool<>(XPathFactory.newInstance()::newXPath);

    static ResourcePool<XPath> getXPathPool() {
        return xPathPool;
    }

    /**
     * Checks according to args
     * @param args The arguments to check based off of
     * @return A stream containing the results of all checks
     *
     * @throws IOException If an i/o error occurs
     * @throws ClassNotFoundException If the book references a class that could not be found
     * @throws SAXException If a SAXException is thrown when parsing the book
     * @throws ClassCastException If the book's section or checkable annotations are not actually annotations, or if their
     *              {@code value()} methods don't return {@code int}s.
     * @throws NoSuchMethodException If the book's section or checkable annotations don't have a {@code value()} method
     * @throws IllegalAccessException If the book's section or checkable annotations' {@code value()} method is not accessible
     * @throws InvocationTargetException If the book's section or checkable annotations' {@code value()} method throws an exception
     */
    @SuppressWarnings("RedundantThrows")
    public static Stream<Result<TestStatus>> check(CheckArgs args) throws IOException, ClassNotFoundException,
            SAXException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Document document = args.tests().getDocument();

        XPath xpath1 = xPathPool.get();
        Class<? extends Annotation> sectionAnnotation;
        NodeList checkableAnnotations;
        try {
            //noinspection unchecked
            sectionAnnotation = (Class<? extends Annotation>) ClassLoader.getPlatformClassLoader().loadClass(
                (String) xpath1.evaluate("/book/meta/sectionType/@annotation", document, XPathConstants.STRING));
            checkableAnnotations = (NodeList) xpath1.evaluate("/book/meta/checkableType", document, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        finally {
            xPathPool.offer(xpath1);
        }
        Method m = sectionAnnotation.getMethod("value");

        int chapter;
        AtomicInteger detectedChapter = new AtomicInteger(-1);
        List<Class<?>> classes = new PathClassLoader(args.paths()).loadAllOwnClasses()
            .filter(clazz -> Arrays.stream(clazz.getAnnotationsByType(sectionAnnotation))
                .map(FunctionUtils.unchecked((FunctionThrowsException<Annotation, Integer>) a -> {
                    m.trySetAccessible();
                    return (int) m.invoke(a);
                }))
                .anyMatch(c -> {
                    if (args.section().isPresent())
                        return c == args.section().getAsInt();
                    else if (detectedChapter.compareAndExchange(-1, c) != c)
                        throw new IllegalArgumentException("Cannot auto detect section, as classes belonging to chapters "
                            + c + " and " + detectedChapter + " were found");
                    else
                        return true;
                })
            )
            .collect(Collectors.toList());
        chapter = args.section().orElseGet(detectedChapter::get);

        XPath xpath2 = xPathPool.get();
        Node ch;
        try {
            ch = (Node) xpath2.evaluate("/book/chapter[@num='" + chapter + "']", document, XPathConstants.NODE);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        finally {
            xPathPool.offer(xpath2);
        }

        UnaryOperatorThrowsIOException<Path> resolver = args.tests()::resolve;

        return Util.streamNodeList(checkableAnnotations)
            .map(Element.class::cast)
            .map(FunctionUtils.unchecked((FunctionThrowsException<Element, CheckableType<?>>) CheckableType::new))
            .map(a -> new TargetGroup<>(a, args.checkables().get(a.annotation()), resolver))
            .flatMap(FunctionUtils.unchecked((FunctionThrowsException<TargetGroup<?>, Stream<Test>>) (e -> {
                e.addPotentialTargets(classes.iterator());
                return e.apply(ch);
            })))
            .map(Test::run);
    }

    private static class TargetGroup<T extends Annotation> {
        private final Targets[] targets;
        private final CheckableType<T> checkableType;
        private final UnaryOperatorThrowsIOException<Path> resolver;

        public TargetGroup(CheckableType<T> checkableType, boolean[] initWhich, UnaryOperatorThrowsIOException<Path> resolver) {
            targets = new Targets[initWhich.length];
            for (int i = 0; i < initWhich.length; i++) {
                if (initWhich[i])
                    targets[i] = new Targets();
            }

            this.checkableType = checkableType;
            this.resolver = resolver;
        }

        public void addPotentialTarget(AnnotatedElement e) throws InvocationTargetException, IllegalAccessException {
            Optional.ofNullable(targets[checkableType.value(e.getAnnotation(checkableType.annotation()))])
                .ifPresent(t -> t.add(e));
        }

        @SuppressWarnings("RedundantThrows")
        public void addPotentialTargets(Iterator<? extends AnnotatedElement> iter) throws InvocationTargetException, IllegalAccessException {
            iter.forEachRemaining(FunctionUtils.unchecked(this::addPotentialTarget));
        }

        public Stream<Test> apply(Node tests) {
            Stream.Builder<Test> builder = Stream.builder();
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null) {
                    String testName = checkableType.name() + ' ' + i;

                    if (targets[i].isEmpty()) {
                        builder.add(Test.withFixedResult(new Result<>(testName, TestStatus.INCOMPLETE)));
                    }
                    else {
                        XPath xpath = null;
                        try {
                            builder.add(Test.multiTest(testName, targets[i],
                                Optional.ofNullable(
                                    (xpath = Checker.getXPathPool().get())
                                        .evaluate(checkableType.name() + "[@num='" + i + "']", tests, XPathConstants.NODE))
                                    .map(Element.class::cast)
                                    .orElseThrow(() -> new IllegalArgumentException("Unable to find tests for " + testName)), resolver));
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
