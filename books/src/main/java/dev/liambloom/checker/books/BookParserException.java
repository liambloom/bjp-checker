package dev.liambloom.checker.books;

/**
 * An exception thrown in the parsing the file(s) containing the book. Used as a wrapper
 * for checked exceptions. For invalid information, this should NOT be used, something else,
 * such as an {@link IllegalStateException} should be used instead.
 */
public class BookParserException extends Exception {
    public BookParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public BookParserException(Throwable cause) {
        super(cause);
    }
}
