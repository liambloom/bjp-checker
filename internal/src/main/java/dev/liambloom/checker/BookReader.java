package dev.liambloom.checker;

import dev.liambloom.checker.internal.*;
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
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
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

        System.loadLibrary("native");
    }

    private final DocumentBuilder db;

    public BookReader() {
        try {
            db = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T validateDocumentAndReturn(Book book, Class<T> clazz) throws IOException, SAXException, ClassNotFoundException {
        if (!clazz.equals(Document.class) && !clazz.equals(Result.class))
            throw new IllegalArgumentException("validateDocumentAndReturn cannot return " + clazz.getName());
        boolean returnDocument = clazz.equals(Document.class);
        if (!book.exists()) {
            if (returnDocument)
                throw new IllegalStateException("Book " + book + " does not exist");
            else
                return (T) new Result<>(book.toString(), TestValidationStatus.NOT_FOUND);
        }
        ErrorHandler handler = returnDocument ? new DefaultHandler2() : new ValidationErrorHandler(book.toString());
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
                    ((ValidationErrorHandler) handler).getLogs());
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
            ValidationErrorHandler h = (ValidationErrorHandler) handler;
            typeErrors.forEach(e -> h.log(System.Logger.Level.ERROR, e));
        }

        if (!book.supportsResourceLoading()
            && (document.getElementsByTagName("File").getLength() > 0 || document.getElementsByTagName("Path").getLength() > 0)) {
            UnsupportedOperationException e =  new UnsupportedOperationException("Book document contains <File> or <Path> element, but Book does not support file resolution");
            if (returnDocument)
                throw e;
            else
                ((ValidationErrorHandler) handler).log(System.Logger.Level.ERROR, e);
        }

        if (returnDocument)
            return (T) document;
        else {
            ValidationErrorHandler h = (ValidationErrorHandler) handler;
            if (h.getMaxErrorKind() == null)
                return (T) new Result<>(book.toString(), TestValidationStatus.VALID);
            else if (h.getMaxErrorKind() == System.Logger.Level.WARNING)
                return (T) new Result<>(book.toString(), TestValidationStatus.VALID_WITH_WARNINGS, h.getLogs());
            else
                return (T) new Result<>(book.toString(), TestValidationStatus.INVALID, h.getLogs());
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

        Class<? extends Annotation> sectionAnnotation;
        NodeList checkableAnnotations;
        Stream<String> resources;
        {
            XPath xpath1 = Util.getXPathPool().get();
            try {
                //noinspection unchecked
                sectionAnnotation = (Class<? extends Annotation>) ClassLoader.getPlatformClassLoader().loadClass(
                    (String) xpath1.evaluate("/book/meta/sectionType/@annotation", document, XPathConstants.STRING));
                checkableAnnotations = (NodeList) xpath1.evaluate("/book/meta/checkableType", document, XPathConstants.NODESET);

                resources = Util.streamNodeList((NodeList) xpath1.evaluate("/book/meta/rsc", document, XPathConstants.NODESET))
                    .map(Element.class::cast)
                    .map(e -> e.getAttribute("href"));
            }
            catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
            finally {
                Util.getXPathPool().offer(xpath1);
            }
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

        changeDirectory(tests.loadResources(Files.createTempDirectory(null), resources));

        XPath xpath2 = Util.getXPathPool().get();
        Node ch;
        try {
            ch = (Node) xpath2.evaluate("/book/chapter[@num='" + chapter + "']", document, XPathConstants.NODE);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        finally {
            Util.getXPathPool().offer(xpath2);
        }

        return Util.streamNodeList(checkableAnnotations)
            .map(Element.class::cast)
            .map(FunctionUtils.unchecked((FunctionThrowsException<Element, CheckableType<?>>) CheckableType::new))
            .map(a -> new CheckerTargetGroup<>(a, checkables.get(a.annotation())))
            .flatMap(FunctionUtils.unchecked((FunctionThrowsException<CheckerTargetGroup<?>, Stream<Test>>) (e -> {
                e.addPotentialTargets(classes.iterator());
                return e.apply(ch);
            })))
            .map(Test::run);
    }

    private static void changeDirectory(Path path) {
        changeDirectory(path.toString());
    }

    private static native void changeDirectory(String path); // TODO
}
