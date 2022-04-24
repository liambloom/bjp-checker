package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.ReLogger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BinaryOperator;

class ValidationErrorHandler implements ErrorHandler {
    private static final BinaryOperator<System.Logger.Level> max = BinaryOperator.maxBy(Comparator.naturalOrder());
    private final ReLogger logger = new ReLogger(System.identityHashCode(this) + "");
    private System.Logger.Level maxErrorKind = System.Logger.Level.ALL;
    private final String book;

    public ValidationErrorHandler(String book) {
        this.book = book;
    }

    public void log(System.Logger.Level level, Throwable e) {
        log(level, null, e);
    }

    private void log(System.Logger.Level level, String prepend, Throwable e) {
//        logger.log(System.Logger.Level.TRACE, "%s %s", level, e);
        maxErrorKind = max.apply(maxErrorKind, level);
        logger.log(level, Optional.ofNullable(prepend).orElse("")
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(baos));
        return e.getClass().getName() + ": " + e.getMessage() + "\n" + baos;
    }

    public System.Logger.Level getMaxErrorKind() {
        return maxErrorKind;
    }

    public ReLogger getLogs() {
        return logger;
    }
}
