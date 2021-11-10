package dev.liambloom.checker;

/**
 * An error thrown when a program attempts to access a part of the checker that
 * is not yet implemented. Will be removed in version 1.0.
 */
public class NotYetImplementedError extends Error {
    public NotYetImplementedError() {
        super();
    }

    public NotYetImplementedError(String message) {
        super(message);
    }

    public NotYetImplementedError(Throwable cause) {
        super(cause);
    }

    public NotYetImplementedError(String message, Throwable cause) {
        super(message, cause);
    }
}
