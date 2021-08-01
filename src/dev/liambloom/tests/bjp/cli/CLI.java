package dev.liambloom.tests.bjp.cli;

import dev.liambloom.tests.bjp.shared.*;
import dev.liambloom.tests.bjp.gui.GUI;
import javafx.application.Application;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CLI {
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
                case "-h":
                case "--help":
                    assertArgsPresent(args, 1);
                    printHelp("checker");
                    break;
                case "-v":
                case "--version":
                    assertArgsPresent(args, 1);
                    System.out.println(App.VERSION);
                    break;
                case "check":
                    try {
                        App.check(CheckArgs.fromCLIArgs(args, 1));
                    }
                    catch (SAXException e) {
                        // TODO: There is probably a better way to do this (should I add a ErrorHandler to Book or CheckArgs?)
                        throw new UserErrorException(e);
                    }
                    //throw new UserErrorException("Command `check' not supported in current checker version");
                    break;
                case "submit":
                    throw new UserErrorException("Command `submit' not supported in current checker version");
                    // break;
                case "tests":
                    if (args.length == 1)
                        throw new UserErrorException("Missing argument, expected one of: add, remove, rename, list, validate"); // TODO
                    switch (args[1]) {
                        // TODO: handle errors
                        case "add":
                            assertArgsPresent(args, 2, "name", "path");
                            Book.addTest(args[2], new Glob(args[3]).single());
                            break;
                        case "remove":
                            Book.removeTest(args[2]);
                            break;
                        case "rename":
                            assertArgsPresent(args, 2, "old-name", "new-name");
                            if (Book.getTest(args[2]) instanceof ModifiableBook book)
                                book.setName(args[3]);
                            else
                                throw new UserErrorException("Book `" + args[2] + "' can't be renamed");
                            break;
                        case "list":
                            assertArgsPresent(args, 2);
                            List<Book> books = Book.getAllTests().collect(Collectors.toList());
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
                            break;
                        case "validate":
                            try {
                                printResults((args[2].equals("-a") || args[2].equals("--all")
                                        ? Book.getAllTests()
                                        : Arrays.stream(args).skip(2).map(Book::getTest))
                                        .map((FunctionThrowsIOException<Book, Result>) Book::validate));
                            }
                            catch (UncheckedIOException e) {
                                throw e.getCause();
                            }
                            break;
                    }
                    break;
                case "gui":
                    Application.launch(GUI.class, Arrays.copyOfRange(args, 1, args.length));
                    break;
                default:
                    throw new UserErrorException("Command `" + args[0] + "' not recognized. See `checker --help' for a list of commands.");
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
        if (arg.contains("/") || (stream = CLI.class.getResourceAsStream("/help/" + arg + ".txt")) == null)
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
                System.out.printf("%s ... \u001b[%sm%s\u001b[0m%n", r.name(), r.status().color().ansi(), r.status().getName());
                r.console().ifPresent((ConsumerThrowsIOException<ByteArrayOutputStream>) (c -> c.writeTo(System.out)));
            });
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
