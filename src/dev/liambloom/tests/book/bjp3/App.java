package dev.liambloom.tests.book.bjp3;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Scanner;

class App {
    public static final String VERSION = "v1.0.0-alpha-1";
    //public static final Pattern COMBINING_FLAG = Pattern.compile("-(?!-).{2,}");

    public static App app;
    public final String here;
    public static final Debugger debugger = new Debugger();

    public App() throws URISyntaxException {
        File f = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (f.isFile())
            here = f.getParent();
        else
            here = f.getPath();
    }

    public static void main(String[] args) {

        try {
            // -[x] Initialization
            app = new App();

            if (args.length == 0) {
                args = new String[] { "--help" };
                System.out.println("Checker " + VERSION + " (C) Liam Bloom 2021" + System.lineSeparator());
            }

            if (args[0].startsWith("-")) {
                switch (mapOption(args[0])) {
                    case "help":
                        lastArg(args, 0);
                        help("check");
                        return;
                    case "version":
                        lastArg(args, 0);
                        System.out.println(VERSION);
                        return;
                }
            }

            if (args[1].startsWith("-") && mapOption(args[1]).equals("help")) {
                lastArg(args, 1);
                if (args[0].equals("glob"))
                    help("glob");
                else if (args[0].startsWith("-"))
                    help(mapOption(args[0]));
                else
                    help(args[0]);
                return;
            }
            // TODO: run tests

            // -[ ] Load Tests
            TestLoader.load();

            // -[x] Load Classes
            // TODO: maybe make an argument to run tests in another directory
            //final Class<?>[] classes = DirectoryClassLoader.loadClassesHere();

            // -[ ] Search classes to find correct chapter(s) and exercise(s)
            // -[ ] Retrieve/decode previous results
            // -[ ] Maybe find some way to store diff and compare (so you don't test
            //          unchanged methods)
            //      Note: Tests would also need to be re-run if dependencies (including java
            //          version) changed
            //      Idea: Use SHA-1 hash to check if files have been changed (this is what git does)
            //          Actually, SHA-1 is broken (has collisions), use something more secure, like
            //          SHA-256
            // -[ ] Run tests in tests.xml
            // -[ ] Print/save/encode/submit results
            //          Results could be: correct, incorrect, previously working, missing
        }
        catch (UserErrorException e) {
            App.debugger.error(e.getMessage());
            if (App.debugger.debugMode && e.getCause() != null)
                e.getCause().printStackTrace();
        }
        catch (Throwable e) {
            // e.printStackTrace();
            App.debugger.internalError(e);
        }
    }

    public static void help(String name) {
        //System.out.println(app.here + File.separator + "help" + File.separator + name + ".txt");
        try (Scanner s = new Scanner(App.class.getResourceAsStream("/help/" + name + ".txt"))) {
            while (s.hasNextLine())
                System.out.println(s.nextLine());
        }
        catch (NullPointerException e) {
            throw new UserErrorException("No help found for `" + name + "'");
        }
    }

    static String mapOption(String option) {
        switch (option) {
            case "-v":
            case "--version":
                return "version";
            case "-h":
            case "--help":
                return "help";
            case "-c":
            case "--chapter":
                return "chapter";
            case "-e":
            case "--exercise":
            case "--exercises":
                return "exercises";
            case "--pp":
            case "--programming-projects":
            case "--programmingProjects":
                return "programmingProjects";
            case "-r":
            case "--results":
                return "results";
            case "-s":
            case "--submit":
                return "submit";
            default:
                throw new UserErrorException("Unknown flag `" + option + "'. Run check --help for a list of valid flags");
        }
    }

    static void lastArg(String[] args, int i) {
        if (args.length > i + 1)
            throw new UserErrorException("Did not expect argument after `" + args[i] + "'");
    }
}
