package dev.liambloom.tests.book.bjp.checker;

import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.util.Arrays;

public class CLI {
    public static void main(String[] args) {
        App.cleanArgs(args);
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
                        new App(logger).check(new CheckArgs(args, 1, logger));
                        //throw new UserErrorException("Command `check' not supported in current checker version");
                        break;
                    case "submit":
                        throw new UserErrorException("Command `submit' not supported in current checker version");
                        // break;
                    case "validate":
                        logger.printResults(new App(logger).validateTests(Arrays.copyOfRange(args, 1, args.length)));
                        break;
                    /*case "results":
                        throw new UserErrorException("Command `results' not supported in current checker version");
                        // break;*/
                    case "gui":
                        GUI.main(Arrays.copyOfRange(args, 1, args.length));
                        break;
                    default:
                        throw new UserErrorException("Command `" + args[0] + "' not recognized. See `checker --help' for a list of commands.");
                }
            }
        }
        catch (UserErrorException e) {
            logger.error(e.getMessage());
            //e.printStackTrace();
        }
        catch (Throwable e) {
            logger.error("An error was encountered internally. Check logs for more information");
            //e.printStackTrace();
            try {
                App.createLogFile(e);
            }
            catch (IOException ignored) {
                logger.error("Failed to create log file");
            }
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
    public <T extends Result> void printResult(T r) {
        System.out.printf("%s ... \u001b[%sm%s\u001b[0m%n", r.name, r.variant.color().ansi, r.variant.getName());
        if (r.variant.isError()) {
            try {
                r.printToStream(System.out);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void close() {
        AnsiConsole.systemUninstall();
    }
}
