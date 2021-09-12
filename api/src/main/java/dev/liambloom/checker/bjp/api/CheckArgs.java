package dev.liambloom.checker.bjp.api;

import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * This represents the arguments for the checking functionality of
 * this program, which can be found at {@link Checker#check(CheckArgs)}
 *
 * @param chapter             The chapter to check, or {@code OptionalInt.empty()} to auto-detect.
 * @param exercises           If (and only if) {@code exercises[i]} is true, then exercise {@code i + 1} will be run.
 * @param programmingProjects If (and only if) {@code programmingProjects[i]} is true, then programming project {@code i + 1} will be run.
 * @param tests               The document containing the tests, which must follow the schema TODO: add link to schema
 * @param paths               A stream of the paths for all .class and .jar files to check
 */
public record CheckArgs(OptionalInt chapter, boolean[] exercises, boolean[] programmingProjects, Document tests,
                        Stream<Path> paths) {
    public static final String DEFAULT_TEST_NAME = "bjp3";
}
