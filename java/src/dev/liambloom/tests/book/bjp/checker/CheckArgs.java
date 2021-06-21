package dev.liambloom.tests.book.bjp.checker;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This represents the arguments for the checking functionality of
 * this program, which can be found at {@link App#check(CheckArgs)}
 */
public class CheckArgs {
    private static final Pattern RANGED_NUM = Pattern.compile("(?:\\d+(?:-\\d+)?(?:,|$))+");
    public static final int MAX_EX_COUNT = 24;
    public static final int MAX_PP_COUNT = 8;
    public static final int MAX_CH = 18;

    private OptionalInt chapter = OptionalInt.empty();
    private boolean[] exercises = null;
    private boolean[] programmingProjects;
    private Document tests;
    private Glob glob;

    /**
     * Constructs CheckArgs from string arguments, beginning
     * at argument {@code i}.
     *
     * @param args The string arguments
     * @param i The position of the first argument
     */
    public CheckArgs(String[] args, int i, Logger logger) throws IOException, SAXException, ParserConfigurationException {
        List<String> globArgs = new LinkedList<>();
        String tests = null;

        for (; i < args.length; i++) {
            switch (args[i]) {
                case "-c":
                case "--chapter":
                    if (chapter.isPresent())
                        throw new UserErrorException("Repeat argument: " + args[i]);
                    try {
                        chapter = OptionalInt.of(Integer.parseInt(args[++i]));
                    }
                    catch (NumberFormatException e) {
                        throw new UserErrorException(e);
                    }
                    break;
                case "-e":
                case "--exercise":
                case "--exercises":
                    if (exercises != null)
                        throw new UserErrorException("Repeat argument: " + args[i]);
                    exercises = new boolean[MAX_EX_COUNT];
                    i = putRanges(args, exercises, i, "exercise");
                    break;
                case "--pp":
                case "--programming-project":
                case "--programmingProject":
                case "--programming-projects":
                case "--programmingProjects":
                    if (programmingProjects != null)
                        throw new UserErrorException("Repeat argument: " + args[i]);
                    programmingProjects = new boolean[MAX_PP_COUNT];
                    i = putRanges(args, programmingProjects, i, "programming project");
                    break;
                case "-t":
                case "--tests":
                    if (tests != null)
                        throw new UserErrorException("Repeat argument: " + args[i]);
                    tests = args[++i];
                    break;
                default:
                    globArgs.add(args[i]);
            }
        }

        if (exercises == null && programmingProjects == null)
            throw new UserErrorException("No exercises or programming projects specified");
        if (tests == null)
            tests = "bjp3";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setSchema(App.loadTestSchema());
        Document document = dbf.newDocumentBuilder().parse(new Glob(Collections.singleton(tests), true, logger).single());

        glob = new Glob(globArgs, false, logger);
    }

    private static int putRanges(String[] args, boolean[] nums, int i, String name) {
        while (RANGED_NUM.matcher(args[++i]).matches()) {
            for (String s : args[i].split(",")) {
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

                if (min > max || max > nums.length || min <= 0)
                    throw new UserErrorException("Range " + s + " is invalid");

                for (int j = min; j <= max; j++) {
                    if (nums[j])
                        throw new UserErrorException("Attempt to list " + name + " " + j + " twice");
                    else
                        nums[j] = true;
                }
            }
        }
        return i;
    }

    public OptionalInt chapter() {
        return chapter;
    }

    public boolean[] exercises() {
        return exercises;
    }

    public boolean[] programmingProjects() {
        return programmingProjects;
    }

    public Glob glob() {
        return glob;
    }

    public Document tests() {
        return tests;
    }
}
