package dev.liambloom.tests.bjp.cli;

import dev.liambloom.tests.bjp.shared.Logger;
import dev.liambloom.tests.bjp.shared.Result;
import org.fusesource.jansi.AnsiConsole;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;

public class CLILogger implements Logger, Closeable {
    public CLILogger() {
        AnsiConsole.systemInstall();
    }

    @Override
    public Logger notice(String msg, Object... args) {
        System.err.println("\u001b[36m[notice]\u001b[0m " + String.format(msg, args));
        return this;
    }

    @Override
    public Logger warn(String msg, Object... args) {
        System.err.println("\u001b[33m[warn]\u001b[0m " + String.format(msg, args));
        return this;
    }

    @Override
    public Logger error(String msg, Object... args) {
        System.err.println("\u001b[31m[error]\u001b[0m " + String.format(msg, args));
        return this;
    }

    @Override
    public <T extends Result> void printResult(T r) {
        System.out.printf("%s ... \u001b[%sm%s\u001b[0m%n", r.name, r.variant.color().ansi, r.variant.getName());
        if (r.variant.isError()) {
            try {
                r.printToStream(System.out);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void close() {
        AnsiConsole.systemUninstall();
    }
}
