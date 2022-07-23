package dev.liambloom.checker.ui.cli;

import dev.liambloom.checker.books.Color;
import dev.liambloom.checker.ui.ColorMap;
import org.fusesource.jansi.AnsiConsole;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class PrintStreamLogger implements System.Logger {
    private final String name;
    private final Map<Level, Boolean> config;
    private final PrintStream out;

    public PrintStreamLogger(String name, Map<Level, Boolean> config, PrintStream out) {
        AnsiConsole.systemInstall();
        this.name = name;
        this.config = config;
        this.out = out;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLoggable(Level level) {
        return config.getOrDefault(level, false);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable throwable) {
        String throwableMessage;
        if (isLoggable(Level.TRACE)) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            throwable.printStackTrace(new PrintStream(os));
            throwableMessage = os.toString();
        }
        else
            throwableMessage = throwable.toString();
        log(level, bundle, msg + ": " + throwableMessage);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Object... params) {
        if (isLoggable(level))
            out.printf("\u001b[" + ColorMap.ansi(color(level)) + "m[" + level.name().toLowerCase(Locale.ROOT) + "]\u001b[0m "  + String.join(System.lineSeparator() + " ".repeat(level.name().length() + 3), msg.split("\\R")) + "%n", params);
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
