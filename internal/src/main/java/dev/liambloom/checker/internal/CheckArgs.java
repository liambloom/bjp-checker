package dev.liambloom.checker.internal;

import dev.liambloom.checker.book.Book;

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * This represents the arguments for the checking functionality of
 * this program, which can be found at {@link Checker#check(CheckArgs)}
 *
 * @param section             The section to check, or {@code OptionalInt.empty()} to auto-detect.
 * @param checkables          A map between annotations for checkables and the checkables of that type to run.
 * @param tests               The document containing the tests, which must follow the
 *                            <a href="https://checker.liambloom.dev/bjp/test-schema.xsd">schema</a>
 * @param paths               A stream of the paths for all .class and .jar files to check
 */
public record CheckArgs(OptionalInt section, Map<Class<? extends Annotation>, boolean[]> checkables, Book tests,
                        Stream<Path> paths) {
}
