package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.cli.CLILogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Source;
import javax.xml.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
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

    @Override
    public Result validate() throws IOException {
        if (exists()) {
            Validator v = Books.getValidatorPool().get();
            ValidationErrorHandler handler = new ValidationErrorHandler();
            v.setErrorHandler(handler);

            try {
                v.validate(getSource());
                Document document = getDocument();
                Stream.of(
                        Stream.of("parameter", "throws")
                                .map(document::getElementsByTagName)
                                .flatMap(l -> IntStream.range(0, l.getLength()).mapToObj(l::item))
                                .map(Node::getTextContent),
                        Stream.of("Array", "ArrayList", "LinkedList", "TargetArrayList", "Stack", "HashSet", "TreeSet", "TargetTree")
                                .map(document::getElementsByTagName)
                                .flatMap(l -> IntStream.range(0, l.getLength()).mapToObj(l::item))
                                .map(Element.class::cast)
                                .map(e -> e.getAttribute("elementType")),
                        Stream.of("HashMap", "TreeMap")
                                .map(document::getElementsByTagName)
                                .flatMap(l -> IntStream.range(0, l.getLength()).mapToObj(l::item))
                                .map(Element.class::cast)
                                .flatMap(e -> Stream.of("keyType", "valueType").map(e::getAttribute))
                )
                        .flatMap(Function.identity())
                        .map(String::trim)
                        .forEach(type -> {
                            try {
                                getClass().getClassLoader().loadClass(type);
                            }
                            catch (ClassNotFoundException e) {
                                synchronized (handler) {
                                    handler.error(e);
                                }
                            }
                        });

            } catch (SAXException ignored) {
            }
            finally {
                Books.getValidatorPool().offer(v);
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
    protected abstract Source getSource() throws IOException;

    private class ValidationErrorHandler implements ErrorHandler {
        private ByteArrayOutputStream log;
        private CLILogger logger;
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
            logger.log(LogKind.ERROR, e.getMessage());
        }

        @Override
        public void fatalError(SAXParseException exception) {
            if (maxErrorKind != LogKind.ERROR)
                maxErrorKind = LogKind.ERROR;
            logger.log(LogKind.ERROR, getMessage(exception));
            // TODO: Maybe log something that indicates parsing stopped here?
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
            logger = new CLILogger(new PrintStream(log));
            maxErrorKind = null;
        }
    }
}
