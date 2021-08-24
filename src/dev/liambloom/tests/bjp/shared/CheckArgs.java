package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.cli.Glob;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This represents the arguments for the checking functionality of
 * this program, which can be found at {@link App#check(CheckArgs)}
 *
 * @param chapter The chapter to check, or {@code OptionalInt.empty()} to auto-detect.
 * @param exercises If (and only if) {@code exercises[i]} is true, then exercise {@code i + 1} will be run.
 * @param programmingProjects If (and only if) {@code programmingProjects[i]} is true, then programming project {@code i + 1} will be run.
 * @param tests The document containing the tests, which must follow the schema TODO: add link to schema
 * @param paths A stream of the paths for all .class and .jar files to check
 */
public record CheckArgs(OptionalInt chapter, boolean[] exercises, boolean[] programmingProjects, Document tests, Stream<Path> paths) {
    private static final Pattern RANGED_NUM = Pattern.compile("(?:\\d+(?:-\\d+)?(?:,|$))+");
    public static final String DEFAULT_TEST_NAME = "bjp3";

    /**
     * Constructs CheckArgs from string arguments, beginning
     * at argument {@code i}.
     *
     * @param args The string arguments
     * @param start The position of the first argument
     */
    public static CheckArgs fromCLIArgs(String[] args, int start) throws IOException, SAXException, ParserConfigurationException {
        List<String> globArgs = new LinkedList<>();
        String testName = null;
        OptionalInt chapter = OptionalInt.empty();
        boolean[] exercises = null;
        boolean[] programmingProjects = null;

        Queue<String> argQ = new ArrayDeque<>(Arrays.asList(args).subList(start, args.length));
        //Document tests;
        //

        while (!argQ.isEmpty()) {
            String arg = argQ.remove();

            switch (arg) {
                case "-c", "--chapter" -> {
                    if (chapter.isPresent())
                        throw new UserErrorException("Repeat argument: " + arg);
                    try {
                        chapter = OptionalInt.of(Integer.parseInt(Optional.ofNullable(argQ.poll()).orElseThrow(
                                () -> new UserErrorException("Missing argument: expected a value after " + arg)
                        )));
                    } catch (NumberFormatException e) {
                        throw new UserErrorException(e);
                    }
                }
                case "-e", "--exercise", "--exercises" -> {
                    if (exercises != null)
                        throw new UserErrorException("Repeat argument: " + arg);
                    exercises = putRanges(argQ, "exercise");
                }
                case "--pp", "--programming-project", "--programmingProject", "--programming-projects", "--programmingProjects" -> {
                    if (programmingProjects != null)
                        throw new UserErrorException("Repeat argument: " + arg);
                    programmingProjects = putRanges(argQ, "programming project");
                }
                case "-t", "--tests" -> {
                    if (testName != null)
                        throw new UserErrorException("Repeat argument: " + arg);
                    testName = Optional.ofNullable(argQ.poll()).orElseThrow(() -> new UserErrorException("Missing argument: expected a value after " + arg));
                }
                default -> globArgs.add(arg);
            }
        }

        if (exercises == null && programmingProjects == null)
            throw new UserErrorException("No exercises or programming projects specified");
        if (exercises == null)
            exercises = new boolean[0];
        if (programmingProjects == null)
            programmingProjects = new boolean[0];
        if (testName == null)
            testName = App.prefs().get("selectedTests", DEFAULT_TEST_NAME);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setSchema(Book.getTestSchema());
        Document tests = Book.getTest(testName).getDocument(dbf.newDocumentBuilder());

        Stream<Path> paths = new Glob(globArgs).files();

        return new CheckArgs(chapter, exercises, programmingProjects, tests, paths);
    }

    private static boolean[] putRanges(Queue<String> args, String name) {
        int absMax = Integer.MIN_VALUE;
        List<int[]> ranges = new ArrayList<>(); //[args.length - i][];

        while (!args.isEmpty() && RANGED_NUM.matcher(args.peek()).matches()) {
            for (String s : args.remove().split(",")) {
                if (s.isEmpty())
                    continue;
                int min, max;
                if (s.contains("-")) {
                    String[] range = s.split("-");
                    min = Integer.parseInt(range[0]);
                    max = Integer.parseInt(range[1]);
                }
                else
                    min = max = Integer.parseInt(s);

                if (absMax < max)
                    absMax = max;

                if (min > max || min <= 0)
                    throw new UserErrorException("Range " + s + " is invalid");

                ranges.add(new int[]{min, max});
            }
        }

        if (ranges.isEmpty())
            throw new UserErrorException("Missing argument: expected value(s) after " + name);

        boolean[] nums = new boolean[absMax];

        for (int[] range : ranges) {
            for (int j = range[0]; j <= range[1]; j++) {
                if (nums[j])
                    throw new UserErrorException("Attempt to list " + name + " " + j + " twice");
                else
                    nums[j] = true;
            }
        }

        return nums;
    }
}
