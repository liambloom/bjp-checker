package io.github.liambloom.tests.book.bjp3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Add placeholder values for now
class Arguments {
    public int chapter = -1;
    public final Integer exercise; // I need an "all" value. I need union types
    public final Integer programmingProject;
    public final XMLCheckLevel xmlCheckLevel;
    public boolean debug = false;
    public final String[] commands;

    //public final File targetDir;

    public Arguments(String[] args) {
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

        for (; i < args.length; i++) {
            if (!args[i].startsWith("-") || args[i].equals("--help"))
                throw new UserErrorException("Unexpected subcommand " + args[i] + ", expected flag");
            switch (args[i]) {
                case "-d":
                case "--debug":
                    if (debug)
                        throw new UserErrorException("Duplicate argument: " + args[i]);
                    debug = true;
                    break;
                case "-c":
                case "--chapter":
                    try {
                        if (chapter != -1)
                            throw new UserErrorException("Duplicate argument: " + args[i]);
                        chapter = Integer.parseInt(args[++i]);
                        // Don't check range because other chapters may be defined in tests
                    }
                    catch (NumberFormatException e) {
                        throw new UserErrorException("Expected integer, found\"" + args[i] + '"', e);
                    }
                    break;
                case "-e":
                case "--exercise":
                case "--exercises":

            }
        }
        // TODO: Parse other flags (this may be problematic if flags are command-dependent)
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
