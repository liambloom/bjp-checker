package dev.liambloom.checker.bjp.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class AbstractBook implements Book {
    private String name;

    public AbstractBook(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    private Stream<Element> elementOfType(Document document, String... types) {
        return Arrays.stream(types)
            .map(document::getElementsByTagName)
            .flatMap(Util::streamNodeList)
            .map(Element.class::cast);
    }

    private Stream<ClassNotFoundException> checkDocumentTypes(Document document) {
        return Stream.of(
            elementOfType(document, "parameter", "throws")
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
                        Util.loadClass(new ClassLoader() {
                            @Override
                            protected Class<?> findClass(String name) throws ClassNotFoundException {
                                if (name.equals("this"))
                                    return null;
                                else
                                    throw new ClassNotFoundException(name);
                            }
                        }, type);
                        yield null;
                    }
                    catch (ClassNotFoundException e) {
                        yield e;
                    }
                }
            })
            .filter(Objects::nonNull);
    }

    @Override
    public Result validate() throws IOException {
        if (exists()) {
            Validator v = Books.getValidatorPool().get();
            ValidationErrorHandler handler = new ValidationErrorHandler();
            v.setErrorHandler(handler);

            try {
                v.validate(new StreamSource(getInputStream()));
            }
            catch (SAXException ignored) {
            }
            finally {
                Books.getValidatorPool().offer(v);
            }

            DocumentBuilder db = Books.getDocumentBuilderPool().get();
            Document document = null;
            try {
                document = db.parse(getInputStream());
            }
            catch (SAXException ignored) {
            }
            finally {
                Books.getDocumentBuilderPool().offer(db);
            }
            if (document != null) {
                checkDocumentTypes(document)
                    .forEachOrdered(handler::error);
            }

            if (handler.getMaxErrorKind() == null)
                return new Result(getName(), TestValidationStatus.VALID);
            else if (handler.getMaxErrorKind() == LogKind.WARN)
                return new Result(getName(), TestValidationStatus.VALID_WITH_WARNINGS, Optional.of(handler.getLogs()));
            else
                return new Result(getName(), TestValidationStatus.INVALID, Optional.of(handler.getLogs()));
        }
        else
            return new Result(getName(), TestValidationStatus.NOT_FOUND);
    }

    @Override
    public Document getDocument() throws IOException, SAXException, ClassNotFoundException {
        if (!exists())
            throw new IllegalStateException("Book " + getName() + " does not exist");
        DocumentBuilder db = Books.getDocumentBuilderPool().get();
        Document r;
        try {
            r = db.parse(getInputStream());
        }
        finally {
            Books.getDocumentBuilderPool().offer(db);
        }
        Optional<ClassNotFoundException> e = checkDocumentTypes(r)
            .findAny();
        if (e.isPresent())
            throw e.get();
        return r;
    }

    protected abstract InputStream getInputStream() throws IOException;

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

        public void error(Throwable e) {
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
                .append(AbstractBook.this instanceof PathBook pathBook ? pathBook.getPath() : getName())
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
