package dev.liambloom.checker.ui.cli;

//import dev.liambloom.checker.*;
import dev.liambloom.checker.Checker;
import dev.liambloom.checker.TestStatus;
import dev.liambloom.checker.books.Result;
import dev.liambloom.checker.ui.*;
import dev.liambloom.util.StringUtils;
import javafx.application.Application;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final Pattern RANGED_NUM = Pattern.compile("(?:\\d+(?:-\\d+)?(?:,|$))+");

//    public static void main(String[] args) {
//        StringProperty s = new SimpleStringProperty();
//        BeanBook book = Books.getBook(args[2]);
//        book.name.bind(s);
//        s.set(args[3]);
//        System.out.println(book.getName());
//    }

    public static void main(String[] args) {
        try {
//            Logger.setLogger(new PrintStreamLogger());
            AnsiConsole.systemInstall();
            /*if (args.length > 1 && (args[1].equals("-h") || args[1].equals("--help"))) {
                // TODO: handle help better
                assertLength(args, 2);
                printHelp(args[0]);
            }
            else {*/

            for (int i = 0; i < args.length; i++) {
                if (args[i].startsWith("-d:") || args[i].startsWith("--debug:")) {
                    try {
                        CheckerUILoggerFinder.setDebugConfigString(args[i].split(":", 2)[1]);
                    }
                    catch (IllegalArgumentException e) {
                        throw new UserErrorException(e.getMessage(), e);
                    }
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 0, newArgs, 0, i);
                    System.arraycopy(args, i + 1, newArgs, i, newArgs.length - i);
                    args = newArgs;
                    break;
                }
            }

            if (args.length == 0)
                args = new String[]{ "-h" };

            Data.initialize();

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
                                case "-b", "--book" -> {
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
                        if (testName == null) {
                            testName = Data.userConfig().getDefaultBook().orElse(null);
                                if (testName == null)
                                    throw new UserErrorException("Either provide book argument (`-b') or set a default book");
                        }

                        // TODO: Do something to catch other error (like references to non-existant types)

                        Stream<Path> paths = new Glob(globArgs).files();

                        BookManager.SelfLoadingBook book = getMaybeAnonymousBook(testName);
                        Result<TestStatus>[] result;
//                        BookChecker reader;

//                        do {
//                            reader = new BookChecker(testName, book.getInnerBook());

                            Map<String, String> checkableNameAbbrMap = new HashMap<>();
                            Set<String> names = new HashSet<>();
                            for (String name : book.book().getMeta().checkableTypeNameSet()) {
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

                             result = Checker.check(book.book(), chapter, processedCheckables, paths);
//                        }
//                        while (!reader.validateResults());

                        new ResultPrinter().printResults(result);
                    }
                    catch (NullPointerException | ClassNotFoundException | IllegalArgumentException e) {
                        throw new UserErrorException(e);
                    }
                }
                case "submit" -> throw new UserErrorException("Command `submit' not supported in current checker version");
                // break;
                case "books" -> {
                    if (!new BookManagerCLI(Data.books()).evaluate(args, 1))
                        throw new UserErrorException("Unknown command: `" + args[1] + "'");
                }
                case "parsers" -> {
                    if (!new ParserManagerCLI(Data.parsers()).evaluate(args, 1))
                        throw new UserErrorException("Unknown command: `" + args[1] + "'");
                }
                case "config" -> {
                    if (args.length == 1)
                        throw new UserErrorException("Missing argument: expected one of: get, set, unset");
                    if (args.length == 2)
                        throw new UserErrorException("Missing argument: property");
                    if (!UserConfig.propertyExists(args[2]))
                        throw new UserErrorException("Configuration property \"" + args[2] + "\" does not exist");
                    UserConfig.Property property = UserConfig.getProperty(args[2]);
                    switch (args[1]) {
                        case "set":
                            assertArgsPresent(args, 3, "value");
                            Data.userConfig().set(property, args[3]);
                        case "get":
                            if (args[1].equals("get"))
                                assertArgsPresent(args, 3);
                            System.out.println(property.name + "=" + Data.userConfig().get(property));
                            break;
                        case "unset":
                            assertArgsPresent(args, 3);
                            Data.userConfig().unset(property);
                            break;
                        default:
                            throw new UserErrorException("Command `config " + args[1] + "' not recognized. See `checker config --help; for a list of subcommands.");
                    }
                }
                case "gui" -> Application.launch(dev.liambloom.checker.ui.gui.Main.class, Arrays.copyOfRange(args, 1, args.length));
                default -> throw new UserErrorException("Command `" + args[0] + "' not recognized. See `checker --help' for a list of subcommands.");
            }
            //}
        }
        catch (UserErrorException e) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, e.getMessage());
            System.exit(1);
            //e.printStackTrace();
        }
        catch (Throwable e) {
            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, "An error was encountered internally", e);
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
//            System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "Shutting down. Currently running: " + Thread.getAllStackTraces().keySet());
            AnsiConsole.systemUninstall();
            System.exit(0);
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

    static void assertArgsPresent(String[] args, int i, String... names) {
        int rem = args.length - i;
        if (rem < names.length)
            throw new UserErrorException("Missing argument: " + names[rem]);
        else if (rem > names.length)
            throw new UserErrorException("Unexpected argument: `" + args[i + names.length] + '\'');
    }

    //    private static void BeanBook

    private static BookManager.SelfLoadingBook getMaybeAnonymousBook(String name) throws IOException {
        try {
            return Data.books().get(name);
        }
        catch (NullPointerException ignored) { }
//        catch (BookParserException | URISyntaxException | ClassNotFoundException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }

//        try {
            throw new Error("The current version does not support anonymous books");
//            return Books.getAnonymousBook(__resolveAnonymousBook(name));
//        }
//        catch (IllegalArgumentException e) {
//            System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, "Unable to find saved book " + name);
//            System.exit(1);
//            return null;
//        }
//        catch (BookParserException | URISyntaxException | ClassNotFoundException |
//               NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
    }

    static URL resolveResource(String src) throws IOException {
        // Get URL
        URL url;
        Exception urlError = null;
        try {
            // Should this be originally constructed as a URI?
            url = new URL(src).toURI().normalize().toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            url = null;
            urlError = e;
        }

        // Check if URL exists
        boolean urlExists;
        if (url == null)
            urlExists = false;
        else {
            try {
                url.openConnection().connect();
                urlExists = true;
            }
            catch (FileNotFoundException e) {
                urlExists = false;
            }
        }

//        System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "URL " + (url == null ? "is null" : "is not null"));
//        System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "URL " + (urlExists ? "exists" : "doesn't exist"));
//        if (url != null)
//            System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "url = " + url);

        // Return URL if it exists
        if (urlExists)
            return url;

        // Get path
        Path path;
        InvalidPathException pathError = null;
        try {
            path = Path.of(src);
        }
        catch (InvalidPathException e) {
            path = null;
            pathError = e;
        }

        // Check if path exists
        boolean pathExists = path != null && Files.exists(path, LinkOption.NOFOLLOW_LINKS);
//        System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "Path " + (path == null ? "is null" : "is not null"));
//        System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "Path " + (pathExists ? "exists" : "doesn't exist"));
//        if (path != null)
//            System.getLogger(Main.class.getName()).log(System.Logger.Level.DEBUG, "Path = " + path);

        // Return path if it exists
        if (pathExists)
            return path.toUri().normalize().toURL();

        // Get path from glob (guaranteed to exist if not null)
        Path globPath;
        UserErrorException globPathError = null;
        try {
            globPath = new Glob(src).single();
        }
        catch (UserErrorException e) {
            globPath = null;
            globPathError = e;
        }


//        System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.DEBUG, "Glob " + (globPath == null ? "is null" : "is not null"));

        // Return path from glob if it exists
        if (globPath != null)
            return globPath.toUri().toURL();

        assert globPathError != null;

        System.Logger logger = System.getLogger(Main.class.getName());

        // Return URL if not null & warn
        if (url != null) {
            logger.log(System.Logger.Level.WARNING, "URL \"" + url + "\" does not exist");
            return url;
        }

        // Return path if not null & warn
        if (path != null) {
            logger.log(System.Logger.Level.WARNING, "Path \"" + path + "\" does not exist");
            return path.toUri().toURL();
        }

        logger.log(System.Logger.Level.ERROR, "Unable to parse \"" + src + "\" as URL: " + urlError.getMessage());
        logger.log(System.Logger.Level.ERROR, "Unable to parse \"" + src + "\" as path: " + pathError.getMessage());
        logger.log(System.Logger.Level.ERROR, "Unable to parse \"" + src + "\" as glob: " + globPathError.getMessage());
        throw new IllegalArgumentException("Unable to parse \"" + src + "\" as URL, path, or glob");
    }
}
