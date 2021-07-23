package dev.liambloom.tests.bjp.cli;

import dev.liambloom.tests.bjp.shared.App;
import dev.liambloom.tests.bjp.gui.GUI;
import dev.liambloom.tests.bjp.shared.Logger;
import dev.liambloom.tests.bjp.shared.Result;
import dev.liambloom.tests.bjp.shared.UserErrorException;
import javafx.application.Application;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.util.Arrays;

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
                        Application.launch(GUI.class, Arrays.copyOfRange(args, 1, args.length));
                        break;
                    default:
                        throw new UserErrorException("Command `" + args[0] + "' not recognized. See `checker --help' for a list of commands.");
                }
            }
        }
        catch (UserErrorException e) {
            logger.error(e.getMessage());
            System.exit(1);
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

            System.exit(1);
        }
        finally {
            logger.close();
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

    private static void assertLength(Object[] a, int l) {
        if (a.length != l)
            throw new UserErrorException("Unexpected argument after `" + a[l - 1] + "'");
    }
}
