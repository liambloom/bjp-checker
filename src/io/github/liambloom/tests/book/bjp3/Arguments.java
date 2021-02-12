package io.github.liambloom.tests.book.bjp3;

import java.util.ArrayList;

class Arguments {
    public final String[] commands;

    public Arguments(String[] args) {
        if (args.length == 0) {
            this.commands = new String[] { "--help" };
            return;
        }

        ArrayList<String> commands = new ArrayList<>(args.length);
        int i = 0;

        for (; i < args.length && (!args[i].startsWith("-") || args[i].equals("--help")); i++)
            commands.add(args[i]);
        this.commands = commands.toArray(new String[0]);

        // TODO: Parse other flags (this may be problematic if flags are command-dependent)
    }
}
