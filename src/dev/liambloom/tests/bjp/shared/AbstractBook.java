package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.cli.CLILogger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Source;
import javax.xml.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

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
            } catch (SAXException ignored) {
            }
            Books.getValidatorPool().offer(v);

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
