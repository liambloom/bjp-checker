package io.github.liambloom.tests.book.bjp3;

import java.io.Closeable;
import org.fusesource.jansi.AnsiConsole;

class Debugger implements Closeable {
    public Debugger() {
        AnsiConsole.systemInstall();
    }

    public void error(String msg, Object... args) {
        System.err.println("\u001b[31m[error]\u001b[0m " + String.format(msg, args));
        System.exit(1);
    }

    public void warn(String msg, Object... args) {
        System.err.println("\u001b[33m[warning]\u001b[0m " + String.format(msg, args));
    }

    public void close() {
        AnsiConsole.systemUninstall();
    }
}
