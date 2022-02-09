package dev.liambloom.checker.books;

/**
 * A miscellaneous exception thrown by the parsing of a book.
 */
public class BookParserException extends Exception {
    public BookParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public BookParserException(Throwable cause) {
        super(cause);
    }
}
