package dev.liambloom.tests.book.bjp.checker;

import java.util.stream.Stream;

public interface Logger {
    Logger notice(String msg, Object... args);
    Logger warn(String msg, Object... args);
    Logger error(String msg, Object... args);
    default <T extends ResultVariant> void printResults(Stream<Result<T>> results) {
        results.forEachOrdered(this::printResult);
    }
    <T extends ResultVariant> void printResult(Result<T> result);
}
