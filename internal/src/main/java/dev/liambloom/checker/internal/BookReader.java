package dev.liambloom.checker.internal;

import dev.liambloom.checker.book.Book;
import dev.liambloom.checker.shared.LogKind;
import dev.liambloom.checker.shared.PrintStreamLogger;
import dev.liambloom.checker.shared.Result;
import dev.liambloom.checker.shared.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.*;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
                new StreamSource(AbstractBook.class.getResourceAsStream("/book-schema.xsd"))));
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
            else if (h.getMaxErrorKind() == LogKind.WARN)
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

    public Document getDocument(Book book) throws IOException, ClassNotFoundException, SAXException {
        return validateDocumentAndReturn(book, Document.class);
    }

    private class ValidationErrorHandler implements ErrorHandler {
        private ByteArrayOutputStream log;
        private PrintStreamLogger logger;
        private LogKind maxErrorKind;

        public ValidationErrorHandler() {
            reset();
        }

        @Override
        public void warning(SAXParseException exception) {
            if (maxErrorKind == null)
                maxErrorKind = LogKind.WARN;
            logger.log(LogKind.WARN, getMessage(exception));
        }

        @Override
        public void error(SAXParseException exception) {
            if (maxErrorKind != LogKind.ERROR)
                maxErrorKind = LogKind.ERROR;
            logger.log(LogKind.ERROR, getMessage(exception));
        }

        public void customError(Throwable e) {
            if (maxErrorKind != LogKind.ERROR)
                maxErrorKind = LogKind.ERROR;
            logger.log(LogKind.ERROR, e.getClass().getName() + ": " + e.getMessage());
        }

        @Override
        public void fatalError(SAXParseException exception) {
            if (maxErrorKind != LogKind.ERROR)
                maxErrorKind = LogKind.ERROR;
            logger.log(LogKind.ERROR, "fatal: " + getMessage(exception));
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

        public ByteArrayOutputStream getLogs() {
            return log;
        }

        public LogKind getMaxErrorKind() {
            return maxErrorKind;
        }

        public void reset() {
            log = new ByteArrayOutputStream();
            logger = new PrintStreamLogger(new PrintStream(log));
            maxErrorKind = null;
        }
    }
}
