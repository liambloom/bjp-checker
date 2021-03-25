package io.github.liambloom.tests.book.bjp3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Add placeholder values for now
class Arguments {
    // Compare flags using identity equality (==).
    public static final int MIN_CH_NUM = 1;
    public static final int AUTO_CH_FLAG = MIN_CH_NUM - 1;
    public static final int MIN_EX_NUM = 1;
    public static final int MAX_EX_NUM = 40;
    public static final boolean[] ALL_EX_FLAG = new boolean[0];
    // TODO: More EX flags
    public static final int MIN_PP_NUM = 1;
    public static final int MAX_PP_NUM = 10;
    // TODO: PP Flags


    public final int chapter;
    public final boolean[] exercise; // I need an "all" value. I need union types
    public final boolean[] programmingProject;
    public final XMLCheckLevel xmlCheckLevel;
    public final String[] commands;

    //public final File targetDir;

    public Arguments(String[] args) {
        // TODO: Do help in here

        // Parse arguments, but that's it
        // Don't make things final, initialize them to null
        // Deal with defaults on access
        // Maybe do different things based on the command
        if (args.length == 0) {
            this.commands = new String[] { "--help" };
            return;
        }

        ArrayList<String> commands = new ArrayList<>(args.length);
        int i = 0;

        for (; i < args.length && (!args[i].startsWith("-") || args[i].equals("--help")); i++)
            commands.add(args[i]);
        this.commands = commands.toArray(new String[0]);

        int chapter = AUTO_CH_FLAG;

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

    // Default constructor
    /*public Arguments() {
        this.commands = null;
        //this.targetDir = new File(System.getProperty("user.dir"));
    }*/


    public enum XMLCheckLevel {
        NoCheck,
        TargetOnlyCheck,
        FullCheck
    }
}
