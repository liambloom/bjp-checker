package dev.liambloom.tests.book.bjp.checker;

import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.util.Iterator;
import java.util.stream.Stream;

public class CLI {
    public static void main(String[] args) {
        CLILogger logger = new CLILogger();

        try {
            if (args.length == 0) {
                printHelp("checker");
            }
            else if (args.length > 1 && (args[1].equals("-h") || args[1].equals("--help"))) {
                assertLength(args, 2);
                printHelp(args[0]);
            }
            else {
                switch (args[0]) {
                    case "-h":
                    case "--help":
                        assertLength(args, 1);
                        printHelp("checker");
                        break;
                    case "-v":
                    case "--version":
                        assertLength(args, 1);
                        System.out.println(App.VERSION);
                        break;
                    case "check":
                        throw new UserErrorException("Command `check' not supported in current checker version");
                        // break;
                    case "submit":
                        throw new UserErrorException("Command `submit' not supported in current checker version");
                        // break;
                    case "validate":
                        final String[] glob = new String[args.length - 1];
                        System.arraycopy(args, 1, glob, 0, glob.length);
                        printResults(new App(logger).validateTests(glob));
                        break;
                    case "results":
                        throw new UserErrorException("Command `results' not supported in current checker version");
                        // break;
                    case "gui":
                        final String[] guiArgs = new String[args.length - 1];
                        System.arraycopy(args, 1, guiArgs, 0, guiArgs.length);
                        GUI.main(guiArgs);
                        break;
                    default:
                        throw new UserErrorException("Command `" + args[0] + "' not recognized. See `checker --help' for a list of commands.");
                }
            }
        }
        catch (UserErrorException e) {
            logger.error(e.getMessage());
        }
        catch (Throwable e) {
            logger.error("An error was encountered internally. Check logs for more information");
            App.createLogFile(e);
        }
        finally {
            logger.close();
        }
    }

    private static void printHelp(String arg) throws IOException {
        String name;
        switch (arg) {
            case "checker":
                name = arg;
                break;
            default:
                throw new UserErrorException("Unable to find help for `" + arg + "'");
        }
        InputStream stream = CLI.class.getResourceAsStream("/help/" + name + ".txt");
        assert stream != null;
        int next;
        while ((next = stream.read()) != -1)
            System.out.write(next);
    }

    private static void assertLength(Object[] a, int l) {
        if (a.length != l)
            throw new UserErrorException("Unexpected argument after `" + a[l - 1] + "'");
    }

    private static <T extends ResultVariant> void printResults(Stream<Result<T>> results) {
        Iterator<Result<T>> iter = results.iterator();
        while (iter.hasNext()) {
            Result<T> r = iter.next();
            // TODO: Add support for more colors
            // TODO: replace PascalCase in r.variant with lowercase+spaces
            System.out.printf("%s ... \u001b[3%dm%s\u001b[0m%n", r.name, r.variant.isOk() ? 2 : 1, r.variant);
            if (r.variant.isError() && r.error != null)
                // TODO: maybe print full stack trace (but only for tests, not validation results)
                System.out.println(r.error.getMessage());
        }
    }
}

class CLILogger implements Logger, Closeable {
    public CLILogger() {
        AnsiConsole.systemInstall();
    }

    @Override
    public Logger notice(String msg, Object... args) {
        System.err.println("\u001b[36m[notice]\u001b[0m " + String.format(msg, args));
        return this;
    }

    @Override
    public Logger warn(String msg, Object... args) {
        System.err.println("\u001b[33m[warn]\u001b[0m " + String.format(msg, args));
        return this;
    }

    @Override
    public Logger error(String msg, Object... args) {
        System.err.println("\u001b[31m[error]\u001b[0m " + String.format(msg, args));
        return this;
    }

    @Override
    public void close() {
        AnsiConsole.systemUninstall();
    }
}
