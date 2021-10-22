package dev.liambloom.checker;

import dev.liambloom.checker.book.Book;
import dev.liambloom.checker.internal.*;
import dev.liambloom.util.ResourcePool;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Note: this is NOT thread safe.
 */
public class BookReader {
    private static final ClassLoader classLoaderAcceptsThis = new ClassLoader() {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals("this"))
                return null;
            else
                throw new ClassNotFoundException(name);
        }
    };
    private static final DocumentBuilderFactory dbf;
    private static final ResourcePool<XPath> xPathPool = new ResourcePool<>(XPathFactory.newInstance()::newXPath);

    static {
        dbf = DocumentBuilderFactory.newInstance();
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        try {
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
            dbf.setSchema(factory.newSchema(
                new StreamSource(BookReader.class.getResourceAsStream("/book-schema.xsd"))));
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        dbf.setNamespaceAware(true);
    }

    private final DocumentBuilder db;
    private Book currentBook;

    public BookReader() {
        try {
            db = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    static ResourcePool<XPath> getXPathPool() {
        return xPathPool;
    }

    @SuppressWarnings("unchecked")
    private <T> T validateDocumentAndReturn(Book book, Class<T> clazz) throws IOException, SAXException, ClassNotFoundException {
        currentBook = book;
        if (!clazz.equals(Document.class) && !clazz.equals(Result.class))
            throw new IllegalArgumentException("validateDocumentAndReturn cannot return " + clazz.getName());
        boolean returnDocument = clazz.equals(Document.class);
        if (!book.exists()) {
            if (returnDocument)
                throw new IllegalStateException("Book " + book + " does not exist");
            else
                return (T) new Result<>(book.toString(), TestValidationStatus.NOT_FOUND);
        }
        ErrorHandler handler = returnDocument ? new DefaultHandler2() : new ValidationErrorHandler();
        db.setErrorHandler(handler);
        Document document;
        try {
            document = db.parse(book.getInputStream());
        }
        catch (SAXException e) {
            if (returnDocument)
                throw e;
            else
                return (T) new Result<>(book.toString(),
                    TestValidationStatus.INVALID,
                    Optional.of(((ValidationErrorHandler) handler).getLogs()));
        }
        // I'd like to wrap exceptions in SAXParseException, but I have no way to get the line/column number
        //  without using a SAX parser. Since I want a document to end with, I would need to either:
        //  a: parse it twice, or b: build my own DocumentBuilder.
        Stream<Exception> typeErrors = Stream.concat(
            Stream.of(
                elementOfType(document, "parameter")
                    .map(Node::getTextContent),
                elementOfType(document, "Array", "ArrayList", "LinkedList", "TargetArrayList", "Stack", "HashSet", "TreeSet", "TargetTree")
                    .map(e -> e.getAttribute("elementType")),
                elementOfType(document, "HashMap", "TreeMap")
                    .flatMap(e -> Stream.of("keyType", "valueType").map(e::getAttribute))
            )
                .flatMap(Function.identity())
                .map(String::trim)
                .map(type -> switch (type) {
                    case "byte", "short", "int", "long", "float", "double", "boolean", "char", "this" -> null;
                    default -> {
                        try {
                            Util.loadClass(classLoaderAcceptsThis, type);
                            yield null;
                        }
                        catch (ClassNotFoundException e) {
                            yield e;
                        }
                    }
                })
                .filter(Objects::nonNull),
            elementOfType(document, "throws")
                .map(Node::getTextContent)
                .map(String::trim)
                .map(type -> {
                    try {
                        Throwable.class.cast(ClassLoader.getSystemClassLoader().loadClass(type));
                        return null;
                    }
                    catch (ClassNotFoundException | ClassCastException e) {
                        return e;
                    }
                })
        );
        if (returnDocument) {
            Optional<Exception> oe = typeErrors.findAny();
            if (oe.isPresent()) {
                Exception e = oe.get();
                if (e instanceof ClassCastException cce)
                    throw cce;
                else if (e instanceof ClassNotFoundException cnfe)
                    throw cnfe;
                else
                    throw new IllegalStateException("An exception of an unexpected type was thrown during validation", e);
            }
        }
        else {
            typeErrors.forEach(((ValidationErrorHandler) handler)::customError);
        }

        if (!book.supportsFileResolution()
            && (document.getElementsByTagName("File").getLength() > 0 || document.getElementsByTagName("Path").getLength() > 0)) {
            UnsupportedOperationException e =  new UnsupportedOperationException("Book document contains <File> or <Path> element, but Book does not support file resolution");
            if (returnDocument)
                throw e;
            else
                ((ValidationErrorHandler) handler).customError(e);
        }

        if (returnDocument)
            return (T) document;
        else {
            ValidationErrorHandler h = (ValidationErrorHandler) handler;
            if (h.getMaxErrorKind() == null)
                return (T) new Result<>(book.toString(), TestValidationStatus.VALID);
            else if (h.getMaxErrorKind() == System.Logger.Level.WARNING)
                return (T) new Result<>(book.toString(), TestValidationStatus.VALID_WITH_WARNINGS, Optional.of(h.getLogs()));
            else
                return (T) new Result<>(book.toString(), TestValidationStatus.INVALID, Optional.of(h.getLogs()));
        }
    }

    private Stream<Element> elementOfType(Document document, String... types) {
        return Arrays.stream(types)
            .map(document::getElementsByTagName)
            .flatMap(Util::streamNodeList)
            .map(Element.class::cast);
    }

    @SuppressWarnings("unchecked")
    public Result<TestValidationStatus> validate(Book book) throws IOException {
        try {
            return (Result<TestValidationStatus>) validateDocumentAndReturn(book, Result.class);
        }
        catch (ClassNotFoundException | SAXException e) {
            throw new IllegalStateException("This exception should not have propagated", e);
        }
    }

    /**
     * Checks according to args
     * @param section             The section to check, or {@code OptionalInt.empty()} to auto-detect.
     * @param checkables          A map between annotations for checkables and the checkables of that type to run.
     * @param tests               The document containing the tests, which must follow the
     *                            <a href="https://checker.liambloom.dev/book-schema.xsd">schema</a>
     * @param paths               A stream of the paths for all .class and .jar files to check
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
    public Stream<Result<TestStatus>> check(OptionalInt section, Map<Class<? extends Annotation>, boolean[]> checkables, Book tests,
                                            Stream<Path> paths) throws IOException, ClassNotFoundException,
        SAXException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        // Should this method be moved to BookReader?
        Document document = validateDocumentAndReturn(tests, Document.class);

        XPath xpath1 = getXPathPool().get();
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
            getXPathPool().offer(xpath1);
        }
        Method m = sectionAnnotation.getMethod("value");

        int chapter;
        AtomicInteger detectedChapter = new AtomicInteger(-1);
        List<Class<?>> classes = new PathClassLoader(paths).loadAllOwnClasses()
            .filter(clazz -> Arrays.stream(clazz.getAnnotationsByType(sectionAnnotation))
                .map(FunctionUtils.unchecked((FunctionThrowsException<Annotation, Integer>) a -> {
                    m.trySetAccessible();
                    return (int) m.invoke(a);
                }))
                .anyMatch(c -> {
                    if (section.isPresent())
                        return c == section.getAsInt();
                    else if (detectedChapter.compareAndExchange(-1, c) != c)
                        throw new IllegalArgumentException("Cannot auto detect section, as classes belonging to chapters "
                            + c + " and " + detectedChapter + " were found");
                    else
                        return true;
                })
            )
            .collect(Collectors.toList());
        chapter = section.orElseGet(detectedChapter::get);

        XPath xpath2 = getXPathPool().get();
        Node ch;
        try {
            ch = (Node) xpath2.evaluate("/book/chapter[@num='" + chapter + "']", document, XPathConstants.NODE);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        finally {
            getXPathPool().offer(xpath2);
        }

        UnaryOperatorThrowsIOException<Path> resolver = tests::resolve;

        return Util.streamNodeList(checkableAnnotations)
            .map(Element.class::cast)
            .map(FunctionUtils.unchecked((FunctionThrowsException<Element, CheckableType<?>>) CheckableType::new))
            .map(a -> new TargetGroup<>(a, checkables.get(a.annotation()), resolver))
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
                                    (xpath = getXPathPool().get())
                                        .evaluate(checkableType.name() + "[@num='" + i + "']", tests, XPathConstants.NODE))
                                    .map(Element.class::cast)
                                    .orElseThrow(() -> new IllegalArgumentException("Unable to find tests for " + testName)), resolver));
                        }
                        catch (XPathExpressionException e) {
                            throw new RuntimeException(e);
                        }
                        finally {
                            if (xpath != null)
                                getXPathPool().offer(xpath);
                        }
                    }
                }
            }
            return builder.build();
        }
    }

    private class ValidationErrorHandler implements ErrorHandler {
        private static final AtomicInteger i = new AtomicInteger(0);
        private ReLogger logger;
        private System.Logger.Level maxErrorKind;

        public ValidationErrorHandler() {
            reset();
        }

        @Override
        public void warning(SAXParseException exception) {
            if (maxErrorKind == null)
                maxErrorKind = System.Logger.Level.WARNING;
            logger.log(System.Logger.Level.WARNING, getMessage(exception));
        }

        @Override
        public void error(SAXParseException exception) {
            if (maxErrorKind != System.Logger.Level.ERROR)
                maxErrorKind = System.Logger.Level.ERROR;
            logger.log(System.Logger.Level.ERROR, getMessage(exception));
        }

        public void customError(Throwable e) {
            if (maxErrorKind != System.Logger.Level.ERROR)
                maxErrorKind = System.Logger.Level.ERROR;
            logger.log(System.Logger.Level.ERROR, e.getClass().getName() + ": " + e.getMessage());
        }

        @Override
        public void fatalError(SAXParseException exception) {
            if (maxErrorKind != System.Logger.Level.ERROR)
                maxErrorKind = System.Logger.Level.ERROR;
            logger.log(System.Logger.Level.ERROR, "fatal: " + getMessage(exception));
        }

        private String getMessage(SAXParseException e) {
            StringBuilder message = new StringBuilder(e.getMessage());
            if (message.charAt(message.length() - 1) == '.')
                message.deleteCharAt(message.length() - 1);

            return message.append(" at ")
                .append('`')
                .append(currentBook.toString())
                .append('\'')
                .append(':')
                .append(e.getLineNumber())
                .append(':')
                .append(e.getColumnNumber())
                .toString();
        }

        public System.Logger.Level getMaxErrorKind() {
            return maxErrorKind;
        }

        public ReLogger getLogs() {
            return logger;
        }

        public void reset() {
            logger = new ReLogger(getClass().getName() + "#" + i.getAndIncrement());
            maxErrorKind = null;
        }
    }
}
