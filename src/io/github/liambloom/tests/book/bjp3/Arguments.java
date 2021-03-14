package io.github.liambloom.tests.book.bjp3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Add placeholder values for now
class Arguments {
    public final int chapter = 1;
    public final Integer exercise = 1; // I need an "all" value. I need union types
    public final Integer programmingProject = null;
    public final XMLCheckLevel xmlCheckLevel = XMLCheckLevel.TargetOnlyCheck;

    //public final File targetDir;

    public Arguments(String[] args) {
        // Parse arguments, but that's it
        // Don't make things final, initialize them to null
        // Deal with defaults on access
        // Maybe do different things based on the command
        /*if (args.length == 0) {
            this.commands = new String[] { "--help" };
            return;
        }

        ArrayList<String> commands = new ArrayList<>(args.length);
        int i = 0;

        for (; i < args.length && (!args[i].startsWith("-") || args[i].equals("--help")); i++)
            commands.add(args[i]);
        this.commands = commands.toArray(new String[0]);*/

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
