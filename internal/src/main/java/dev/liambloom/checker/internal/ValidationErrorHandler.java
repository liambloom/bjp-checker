package dev.liambloom.checker.internal;

import dev.liambloom.checker.ReLogger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BinaryOperator;

public class ValidationErrorHandler implements ErrorHandler {
    private static final BinaryOperator<System.Logger.Level> max = BinaryOperator.maxBy(Comparator.naturalOrder());
    private final ReLogger logger = new ReLogger(Util.generateLoggerName());
    private System.Logger.Level maxErrorKind = System.Logger.Level.ALL;
    private final String book;

    public ValidationErrorHandler(String book) {
        this.book = book;
    }

    public void log(System.Logger.Level level, Throwable e) {
        log(level, null, e);
    }

    private void log(System.Logger.Level level, String prepend, Throwable e) {
        maxErrorKind = max.apply(maxErrorKind, level);
        logger.log(level, Optional.of(prepend).orElse("")
            + (e instanceof SAXParseException spe ? getMessage(spe) : getMessage(e)));
    }

    @Override
    public void warning(SAXParseException exception) {
        log(System.Logger.Level.WARNING, exception);
    }

    @Override
    public void error(SAXParseException exception) {
        log(System.Logger.Level.ERROR, exception);
    }

    @Override
    public void fatalError(SAXParseException exception) {
        log(System.Logger.Level.ERROR, "fatal: ", exception);
    }

    private String getMessage(SAXParseException e) {
        StringBuilder message = new StringBuilder(e.getMessage());
        if (message.charAt(message.length() - 1) == '.')
            message.deleteCharAt(message.length() - 1);

        return message.append(" at ")
            .append('`')
            .append(book)
            .append('\'')
            .append(':')
            .append(e.getLineNumber())
            .append(':')
            .append(e.getColumnNumber())
            .toString();
    }

    private String getMessage(Throwable e) {
        return e.getClass().getName() + ": " + e.getMessage();
    }

    public System.Logger.Level getMaxErrorKind() {
        return maxErrorKind;
    }

    public ReLogger getLogs() {
        return logger;
    }
}
