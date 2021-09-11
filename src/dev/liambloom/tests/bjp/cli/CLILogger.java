package dev.liambloom.tests.bjp.cli;

import dev.liambloom.tests.bjp.shared.Color;
import dev.liambloom.tests.bjp.shared.LogKind;
import dev.liambloom.tests.bjp.shared.Logger;
import org.fusesource.jansi.AnsiConsole;

import java.io.Closeable;
import java.io.PrintStream;

public class CLILogger implements Logger, Closeable {
    private final PrintStream out;

    public CLILogger() {
        this(System.err);
    }

    public CLILogger(PrintStream out) {
        this.out = out;
        AnsiConsole.systemInstall();
    }

    @Override
    public void log(LogKind logKind, String msg, Object... args) {
        out.printf("\u001b[%sm[%s]\u001b[0m ",
            (switch (logKind) {
                case ERROR -> Color.RED;
                case WARN -> Color.YELLOW;
                case NOTICE -> Color.CYAN;
                case OTHER -> Color.GRAY;
            }).ansi(),
            logKind.toString().toLowerCase());
        out.printf(msg, args);
        out.println();
    }

    @Override
    public void close() {
        AnsiConsole.systemUninstall();
    }
}
