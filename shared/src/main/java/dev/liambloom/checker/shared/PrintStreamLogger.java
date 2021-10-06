package dev.liambloom.checker.shared;

import java.io.PrintStream;

public class PrintStreamLogger implements Logger {
    private final PrintStream out;

    public PrintStreamLogger() {
        this(System.err);
    }

    public PrintStreamLogger(PrintStream out) {
        this.out = out;
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
}
