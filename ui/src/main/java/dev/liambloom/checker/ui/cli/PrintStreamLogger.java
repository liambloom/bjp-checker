package dev.liambloom.checker.ui.cli;

import dev.liambloom.checker.Color;
import org.fusesource.jansi.AnsiConsole;

import java.io.PrintStream;
import java.util.Locale;
import java.util.ResourceBundle;

public class PrintStreamLogger implements System.Logger {
    private final String name;
    private final boolean debug;
    private final PrintStream out;

    public PrintStreamLogger(String name, boolean debug, PrintStream out) {
        AnsiConsole.systemInstall();
        this.name = name;
        this.debug = debug;
        this.out = out;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLoggable(Level level) {
        return debug || System.Logger.Level.INFO.compareTo(level) <= 0;
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable throwable) {
        log(level, bundle, msg + "%n%s", (Object) throwable);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Object... params) {
        out.printf("\u001b[" + color(level).ansi() + "m[" + level.name().toLowerCase(Locale.ROOT) + "]\u001b[0m "  + String.join(" ".repeat(level.name().length() + 2), msg.split("\\R")) + "%n", params);
    }

    private Color color(Level level) {
        return switch (level) {
            case ALL -> null;
            case TRACE -> Color.BLUE;
            case DEBUG -> Color.MAGENTA;
            case INFO -> Color.CYAN;
            case WARNING -> Color.YELLOW;
            case ERROR -> Color.RED;
            case OFF -> null;
        };
    }

    /*@Override
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
    }*/
}
