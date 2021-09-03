package dev.liambloom.tests.bjp.cli;

import dev.liambloom.tests.bjp.shared.*;
import javafx.application.Application;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        App.setLogger(new CLILogger());

        try {
            /*if (args.length > 1 && (args[1].equals("-h") || args[1].equals("--help"))) {
                // TODO: handle help better
                assertLength(args, 2);
                printHelp(args[0]);
            }
            else {*/
            switch (args.length == 0 ? "-h" : args[0]) {
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
                        Checker.check(CheckArgs.fromCLIArgs(args, 1));
                    } catch (SAXException | ClassNotFoundException e) {
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
                                        .map((FunctionThrowsIOException<Book, Result>) Book::validate));
                            } catch (UncheckedIOException e) {
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
                case "gui" -> Application.launch(dev.liambloom.tests.bjp.gui.Main.class, Arrays.copyOfRange(args, 1, args.length));
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

    public static void printResults(Stream<Result> s) throws IOException {
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
}
