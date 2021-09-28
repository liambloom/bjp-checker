package dev.liambloom.checker.bjp.cli;

import dev.liambloom.checker.bjp.api.*;
import javafx.application.Application;
import org.fusesource.jansi.AnsiConsole;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final Pattern RANGED_NUM = Pattern.compile("(?:\\d+(?:-\\d+)?(?:,|$))+");

    public static void main(String[] args) {
        try {
            App.setLogger(new PrintStreamLogger());
            AnsiConsole.systemInstall();
            /*if (args.length > 1 && (args[1].equals("-h") || args[1].equals("--help"))) {
                // TODO: handle help better
                assertLength(args, 2);
                printHelp(args[0]);
            }
            else {*/
            if (args.length == 0)
                args = new String[]{ "-h" };

            switch (args[0]) {
                case "-h", "--help" -> {
                    assertArgsPresent(args, 1);
                    printHelp("checker");
                }
                case "-v", "--version" -> {
                    assertArgsPresent(args, 1);
                    System.out.println(App.VERSION);
                }
                case "check" -> {
                    try {
                        List<String> globArgs = new LinkedList<>();
                        String testName = null;
                        OptionalInt chapter = OptionalInt.empty();
                        boolean[] exercises = null;
                        boolean[] programmingProjects = null;

                        Queue<String> argQ = new ArrayDeque<>(Arrays.asList(args).subList(1, args.length));

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
                                    }
                                    catch (NumberFormatException e) {
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
                            testName = App.prefs().get("selectedTests", CheckArgs.DEFAULT_TEST_NAME);

                        // TODO: Do something to catch other error (like references to non-existant types)

                        Stream<Path> paths = new Glob(globArgs).files();

                        Checker.check(new CheckArgs(chapter, exercises, programmingProjects, Books.getBook(testName), paths));
                    }
                    catch (SAXException | ClassNotFoundException e) {
                        // TODO: There is probably a better way to do this (should I add a ErrorHandler to Book or CheckArgs?)
                        throw new UserErrorException(e);
                    }
                }
                case "submit" -> throw new UserErrorException("Command `submit' not supported in current checker version");
                // break;
                case "tests" -> {
                    if (args.length == 1)
                        throw new UserErrorException("Missing argument, expected one of: add, remove, rename, list, validate, get-default, set-default"); // TODO
                    switch (args[1]) {
                        // TODO: handle errors
                        case "add" -> {
                            assertArgsPresent(args, 2, "name", "path");
                            Books.addBook(args[2], new Glob(args[3]).single());
                        }
                        case "remove" -> Books.removeBook(args[2]);
                        case "rename" -> {
                            assertArgsPresent(args, 2, "old name", "new name");
                            if (Books.getBook(args[2]) instanceof ModifiableBook book)
                                book.rename(args[3]);
                            else
                                throw new UserErrorException("Book `" + args[2] + "' can't be renamed");
                        }
                        case "change" -> {
                            assertArgsPresent(args, 2, "name", "new path");
                            if (Books.getBook(args[2]) instanceof PathBook book)
                                book.setPath(new Glob(args[3]).single());
                            else
                                throw new UserErrorException("Book `" + args[2] + "' has no path associated with it");
                        }
                        case "list" -> {
                            assertArgsPresent(args, 2);
                            List<Book> books = Books.getAllBooks().collect(Collectors.toList());
                            String[][] names = new String[books.size()][2];
                            int maxBookNameLength = 0;
                            for (int i = 0; i < names.length; i++) {
                                Book book = books.get(i);
                                String name = book.getName();
                                if (name.length() > maxBookNameLength)
                                    maxBookNameLength = name.length();
                                names[i][0] = name;
                                names[i][1] = book instanceof PathBook pathBook ? pathBook.getPath().toString() : "";
                            }
                            for (String[] book : names)
                                System.out.printf("%-" + maxBookNameLength + "s  %s%n", book[0], book[1]);
                        }
                        case "validate" -> {
                            if (args.length == 2)
                                throw new UserErrorException("Missing argument after validate");
                            try {
                                printResults((args[2].equals("-a") || args[2].equals("--all")
                                    ? Books.getAllBooks()
                                    : Arrays.stream(args).skip(2).map(Books::getBook))
                                    .map((FunctionThrowsIOException<Book, Result<TestValidationStatus>>) Book::validate));
                            }
                            catch (UncheckedIOException e) {
                                throw e.getCause();
                            }
                        }
                        case "get-default" -> System.out.println(App.prefs().get("selectedTests", CheckArgs.DEFAULT_TEST_NAME));
                        case "set-default" -> {
                            assertArgsPresent(args, 2, "name");
                            if (!Books.bookNameExists(args[2]))
                                throw new UserErrorException("Tests \"" + args[2] + "\" not found");
                            App.prefs().put("selectedTests", args[2]);
                        }
                        default -> throw new UserErrorException("Command `tests " + args[1] + "' not recognized. See `checker tests --help' for a list of subcommands of `tests'");
                    }
                }
                case "gui" -> Application.launch(dev.liambloom.checker.bjp.gui.Main.class, Arrays.copyOfRange(args, 1, args.length));
                default -> throw new UserErrorException("Command `" + args[0] + "' not recognized. See `checker --help' for a list of commands.");
            }
            //}
        }
        catch (UserErrorException e) {
            App.logger.log(LogKind.ERROR, e.getMessage());
            System.exit(1);
            //e.printStackTrace();
        }
        catch (Throwable e) {
            App.logger.log(LogKind.ERROR, "An error was encountered internally. Check logs for more information");
            //e.printStackTrace();
            try {
                App.createLogFile(e);
            }
            catch (IOException ignored) {
                App.logger.log(LogKind.ERROR, "Failed to create log file");
            }

            System.exit(1);
        }
        finally {
            AnsiConsole.systemUninstall();
        }
    }

    private static void printHelp(String arg) throws IOException {
        InputStream stream;
        if (arg.contains("/") || (stream = Main.class.getResourceAsStream("/help/" + arg + ".txt")) == null)
            throw new UserErrorException("Unable to find help for `" + arg + "'");
        int next;
        while ((next = stream.read()) != -1)
            System.out.write(next);
    }

    private static void assertArgsPresent(String[] args, int i, String... names) {
        int rem = args.length - i;
        if (rem < names.length)
            throw new UserErrorException("Missing argument: " + names[rem]);
        else if (rem > names.length)
            throw new UserErrorException("Unexpected argument: `" + args[i + names.length] + '\'');
    }

    public static void printResults(Stream<Result<?>> s) throws IOException {
        try {
            s.forEachOrdered(r -> {
                System.out.printf("%s ... \u001b[%sm%s\u001b[0m%n", r.name(), r.status().color().ansi(), Case.convert(r.status().toString(), Case.SPACE));
                r.console().ifPresent((ConsumerThrowsIOException<ByteArrayOutputStream>) (c -> c.writeTo(System.out)));
            });
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
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

                ranges.add(new int[]{ min, max });
            }
        }

        if (ranges.isEmpty())
            throw new UserErrorException("Missing argument: expected value(s) after " + name);

        boolean[] nums = new boolean[absMax + 1];

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
