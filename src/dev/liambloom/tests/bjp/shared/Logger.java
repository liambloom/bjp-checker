package dev.liambloom.tests.bjp.shared;

import java.util.stream.Stream;

public interface Logger {
    Logger notice(String msg, Object... args);
    Logger warn(String msg, Object... args);
    Logger error(String msg, Object... args);
    default <T extends Result> void printResults(Stream<T> results) {
        results.forEachOrdered(this::printResult);
    }
    <T extends Result> void printResult(T result);
}
