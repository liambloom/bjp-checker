package dev.liambloom.checker.internal;

import dev.liambloom.checker.book.Book;

import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * This represents the arguments for the checking functionality of
 * this program, which can be found at {@link Checker#check(CheckArgs)}
 *
 * @param chapter             The chapter to check, or {@code OptionalInt.empty()} to auto-detect.
 * @param exercises           If (and only if) {@code exercises[i]} is true, then exercise {@code i} will be run.
 * @param programmingProjects If (and only if) {@code programmingProjects[i]} is true, then programming project {@code i} will be run.
 * @param tests               The document containing the tests, which must follow the
 *                            <a href="https://checker.liambloom.dev/bjp/test-schema.xsd">schema</a>
 * @param paths               A stream of the paths for all .class and .jar files to check
 */
public record CheckArgs(OptionalInt chapter, boolean[] exercises, boolean[] programmingProjects, Book tests,
                        Stream<Path> paths) {
}
