package dev.liambloom.tests.book.bjp3;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Stream;

// Add placeholder values for now
class Arguments {
    // Compare flags using identity equality (==).
    public static final int MIN_CH_NUM = 1;
    public static final int AUTO_CH_FLAG = MIN_CH_NUM - 1;
    public static final int MIN_EX_NUM = 1;
    public static final int MAX_EX_NUM = 40;
    public static final boolean[] ALL_EX_FLAG = new boolean[0];

    /** Tests all exercises not currently marked as "correct" */
    public static final boolean[] MISSING_EX = new boolean[0];
    public static final int MIN_PP_NUM = 1;
    public static final int MAX_PP_NUM = 10;
    // TODO: PP Flags

    //public static final Pattern COMBINING_FLAG = Pattern.compile("-(?!-).{2,}");


    public final int chapter;
    public final boolean[] exercise;
    public final boolean[] programmingProject;
    public final String[] glob;
    //public final String[] commands;

    //public final File targetDir;

    public Arguments(String[] args) {
        // TODO: Do help in here

        // Parse arguments, but that's it
        // Don't make things final, initialize them to null
        // Deal with defaults on access
        // Maybe do different things based on the command
        /*if (args.length == 0) {
            args = new String[] { "--help" };
            return;
        }

        ArrayList<String> flatArgs = new ArrayList<>();

        for (String arg : args) {
            if (COMBINING_FLAG.matcher(arg).matches()) {
                for (int i = 1; i < arg.length(); i++)
                    flatArgs.add("-" + arg.charAt(i));
            }
            else
                flatArgs.add(arg);
            switch (flatArgs.get(flatArgs.size() - 1)) {
                case "-v":
                case "--version":
                    System.out.println(App.VERSION);

            }
        }*/

        //ArrayList<String> commands = new ArrayList<>(args.length);
        int i = 0;

        /*for (; i < args.length && (!args[i].startsWith("-") || args[i].equals("--help")); i++)
            commands.add(args[i]);
        this.commands = commands.toArray(new String[0]);*/

        int chapter = AUTO_CH_FLAG;

        if (args[0].startsWith("tests")) {
            // TODO: validate tests and DON'T check
        }
        else {
            // TODO: run tests
        }

        switch (args[0]) {
            case "check":
                // TODO check
                break;
            case "validate":
        }

        for (; i < args.length; i++) {
            if (!args[i].startsWith("-") || args[i].equals("--help"))
                throw new UserErrorException("Unexpected subcommand " + args[i] + ", expected flag");
            switch (args[i]) {
                case "-c":
                case "--chapter":
                    try {
                        if (chapter != AUTO_CH_FLAG)
                            throw new UserErrorException("Duplicate argument: " + args[i]);
                        chapter = Integer.parseInt(args[++i]);
                        if (chapter <= MIN_CH_NUM)
                            throw new UserErrorException("Chapter cannot be less than 1");
                        // Don't check range because other chapters may be defined in tests
                    }
                    catch (NumberFormatException e) {
                        throw new UserErrorException("Expected integer, found\"" + args[i] + '"', e);
                    }
                    break;
                case "-e":
                case "--exercise":
                case "--exercises":
                    // TODO: Set exercise accordingly
                    break;
                case "--pp":
                case "--programming-project":
                case "--programmingProject":
                    // TODO: Set programmingProject accordingly
                    break;
                // TODO: add flags to set exercises and pps together (eg. --all, --unfinished)
            }
        }
        // TODO: Parse other flags (this may be problematic if flags are command-dependent)

        this.chapter = chapter;
    }

    public static boolean preParser(String[] args) {
        if (args.length == 0) {
            args = new String[] { "--help" };
            System.out.println("Checker " + App.VERSION + " (C) Liam Bloom 2021" + System.lineSeparator());
        }

        if (args[0].startsWith("-")) {
            switch (mapPreOption(args[0])) {
                case "help":
                    lastArg(args, 0);
                    help("check");
                    return true;
                case "version":
                    lastArg(args, 0);
                    System.out.println(App.VERSION);
                    return true;
            }
        }

        if (args[1].startsWith("-") && mapPreOption(args[1]).equals("help")) {
            lastArg(args, 1);
            if (args[0].equals("glob"))
                help("glob");
            else if (args[0].startsWith("-"))
                help(mapOption(args[0]));
            else
                help(args[0]);
            return true;
        }

        if (Arrays.stream(args).anyMatch(((Predicate<String>) "-s"::equals).or("--submit"::equals))) {
            // TODO run tests on server
            return true;
        }

        return false;
    }

    public static void help(String name) {
        InputStream help = App.class.getResourceAsStream("/help/" + name + ".txt");
        if (help == null)
            throw new UserErrorException("No help found for `" + name + "'");
        else {
            Scanner s = new Scanner(help);
            while (s.hasNextLine())
                System.out.println(s.nextLine());
            s.close();
        }
    }

    static String mapPreOption(String option) {
        switch (option) {
            case "-v":
            case "--version":
                return "version";
            case "-h":
            case "--help":
                return "help";
            default:
                return mapOption(option);
        }
    }

    static String mapOption(String option) {
        switch (option) {
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
