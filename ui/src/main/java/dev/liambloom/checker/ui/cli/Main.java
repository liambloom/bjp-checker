package dev.liambloom.checker.ui.cli;

import dev.liambloom.checker.*;
import dev.liambloom.checker.ui.Books;
import dev.liambloom.checker.ui.UserErrorException;
import dev.liambloom.util.StringUtils;
import javafx.application.Application;
import org.fusesource.jansi.AnsiConsole;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final Pattern RANGED_NUM = Pattern.compile("(?:\\d+(?:-\\d+)?(?:,|$))+");

    public static void main(String[] args) {
        try {
//            Logger.setLogger(new PrintStreamLogger());
            AnsiConsole.systemInstall();
            Preferences prefs = Preferences.userRoot().node("dev/liambloom/checker");
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
                    System.out.println(Main.class.getPackage().getImplementationVersion());
                }
                case "check" -> {
                    try {
                        List<String> globArgs = new LinkedList<>();
                        String testName = null;
                        OptionalInt chapter = OptionalInt.empty();
                        Map<String, boolean[]> preCheckables = new HashMap<>();

                        Queue<String> argQ = new ArrayDeque<>(Arrays.asList(args).subList(1, args.length));

                        while (!argQ.isEmpty()) {
                            String arg = argQ.remove();

                            switch (arg) {
                                case "-s", "--section" -> {
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
                                case "-b", "--books" -> {
                                    if (testName != null)
                                        throw new UserErrorException("Repeat argument: " + arg);
                                    testName = Optional.ofNullable(argQ.poll()).orElseThrow(() -> new UserErrorException("Missing argument: expected a value after " + arg));
                                }
                                default -> {
                                    String target;
                                    if (arg.startsWith("-t:"))
                                        target = arg.substring(3);
                                    else if (arg.startsWith("--target:"))
                                        target = arg.substring(9);
                                    else {
                                        globArgs.add(arg);
                                        break;
                                    }
                                    preCheckables.compute(target, (k, v) -> {
                                        if (v == null) {
                                            int absMax = Integer.MIN_VALUE;
                                            List<int[]> ranges = new ArrayList<>(); //[args.length - i][];

                                            while (!argQ.isEmpty() && RANGED_NUM.matcher(argQ.peek()).matches()) {
                                                for (String s : argQ.remove().split(",")) {
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
                                                throw new UserErrorException("Missing argument: expected value(s) after " + arg);

                                            boolean[] nums = new boolean[absMax + 1];

                                            for (int[] range : ranges) {
                                                for (int j = range[0]; j <= range[1]; j++) {
                                                    if (nums[j])
                                                        throw new UserErrorException("Attempt to list " + target + " " + j + " twice");
                                                    else
                                                        nums[j] = true;
                                                }
                                            }

                                            return nums;
                                        }
                                        else
                                            throw new UserErrorException("Duplicate Argument: " + arg);
                                    });
                                }
                            }
                        }

                        if (preCheckables.isEmpty())
                            throw new UserErrorException("No exercises or programming projects specified");
                        if (testName == null)
                            testName = Books.defaultBook().orElseThrow(() -> new UserErrorException("Either provide book argument (`-b') or set a default book"));//prefs.get("selectedTests", CheckArgs.DEFAULT_TEST_NAME);

                        // TODO: Do something to catch other error (like references to non-existant types)

                        Stream<Path> paths = new Glob(globArgs).files();

                        Book book = Books.getBook(testName);
                        Result<TestStatus>[] result;
                        BookReader reader;

                        do {
                            reader = book.getReader();

                            Map<String, String> checkableNameAbbrMap = new HashMap<>();
                            Set<String> names = new HashSet<>();
                            for (String name : reader.getCheckableTypeSet()) {
                                names.add(name);
                                Set<String> variations = Stream.of(StringUtils.Case.PASCAL, StringUtils.Case.CAMEL, StringUtils.Case.SNAKE, StringUtils.Case.CONST, StringUtils.Case.SKEWER)
                                    .map(c -> StringUtils.convertCase(name, c))
                                    .collect(Collectors.toSet());
                                variations.add(name.substring(0, 1));
                                variations.add(String.valueOf(StringUtils.initials(name)));

                                for (String abbr : variations) {
                                    checkableNameAbbrMap.merge(abbr, name, (oldValue, newValue) -> {
                                        String r = null;
                                        if (abbr.equals(newValue))
                                            r = newValue;
                                        else if (names.contains(oldValue))
                                            r = oldValue;
                                        if (preCheckables.containsKey(abbr)) {
                                            if (r == null)
                                                throw new IllegalArgumentException("Ambiguous abbreviation " + abbr + ": could refer to " + oldValue + " or " + newValue);
                                            else
                                                return r;
                                        }
                                        else
                                            return null;
                                    });
                                }
                            }

                            Map<String, boolean[]> processedCheckables = new HashMap<>();
                            for (Map.Entry<String, boolean[]> e : preCheckables.entrySet()) {
                                String k = checkableNameAbbrMap.get(e.getKey());
                                if (k == null)
                                    throw new IllegalArgumentException("Unknown checkable type: " + e.getKey());
                                processedCheckables.merge(k, e.getValue(), (v1, v2) -> {
                                    throw new IllegalArgumentException("Repeat argument: --target:" + k);
                                });
                            }

                             result = reader.check(chapter, processedCheckables, paths);
                        }
                        while (!reader.validateResults());

                        printResults(result);
                    }
                    catch (SAXException | ClassNotFoundException | IllegalArgumentException e) {
                        throw new UserErrorException(e);
                    }
                }
                case "submit" -> throw new UserErrorException("Command `submit' not supported in current checker version");
                // break;
                case "books" -> {
                    if (args.length == 1)
                        throw new UserErrorException("Missing argument, expected one of: add, remove, rename, list, validate, get-default, set-default"); // TODO
                    switch (args[1]) {
                        // TODO: handle errors
                        case "add" -> {
                            assertArgsPresent(args, 2, "name", "path");
                            try {
                                Books.addBook(args[2], new Glob(args[3]).single());
                            }
                            catch (IllegalArgumentException e) {
                                throw new UserErrorException(e);
                            }
                        }
                        case "remove" -> Books.removeBook(args[2]);
                        case "rename" -> {
                            assertArgsPresent(args, 2, "old name", "new name");
                            if (Books.getBook(args[2]) instanceof ModifiableBook book) {
                                try {
                                    book.rename(args[3]);
                                }
                                catch (IllegalArgumentException e) {
                                    throw new UserErrorException(e);
                                }
                            }
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
                        case "get-default" -> System.out.println(prefs.get("selectedTests", CheckArgs.DEFAULT_TEST_NAME));
                        case "set-default" -> {
                            assertArgsPresent(args, 2, "name");
                            if (!Books.bookNameExists(args[2]))
                                throw new UserErrorException("Tests \"" + args[2] + "\" not found");
                            prefs.put("selectedTests", args[2]);
                        }
                        default -> throw new UserErrorException("Command `tests " + args[1] + "' not recognized. See `checker tests --help' for a list of subcommands of `tests'");
                    }
                }
                case "gui" -> Application.launch(dev.liambloom.checker.ui.gui.Main.class, Arrays.copyOfRange(args, 1, args.length));
                default -> throw new UserErrorException("Command `" + args[0] + "' not recognized. See `checker --help' for a list of commands.");
            }
            //}
        }
        catch (UserErrorException e) {
            Logger.logger.log(LogKind.ERROR, e.getMessage());
            System.exit(1);
            //e.printStackTrace();
        }
        catch (Throwable e) {
            Logger.logger.log(LogKind.ERROR, "An error was encountered internally. Check logs for more information");
            //e.printStackTrace();
            /*try {
                Logger.createLogFile(e);
            }
            catch (IOException ignored) {
                Logger.logger.log(LogKind.ERROR, "Failed to create log file");
            }*/

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

    public static void printResults(Result<?>[] s) throws IOException {
        try {
            for (Result<?> r : s) {
                System.out.printf("%s ... \u001b[%sm%s\u001b[0m%n", r.name(), r.status().color().ansi(), StringUtils.convertCase(r.status().toString(), StringUtils.Case.SPACE));
                r.console().ifPresent((ConsumerThrowsIOException<ByteArrayOutputStream>) (c -> c.writeTo(System.out)));
            }
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

}
