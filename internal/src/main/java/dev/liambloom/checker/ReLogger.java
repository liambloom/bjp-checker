package dev.liambloom.checker;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class ReLogger implements System.Logger {
    private final String name;

    private final ArrayList<LogRecord> logs = new ArrayList<>();

    public ReLogger(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLoggable(Level level) {
        return true;
    }

    @Override
    public synchronized void log(Level level, String msg) {
        logs.add(logger -> logger.log(level, msg));
    }

    @Override
    public synchronized void log(Level level, Supplier<String> msgSupplier) {
        logs.add(logger -> logger.log(level, msgSupplier));
    }

    @Override
    public synchronized void log(Level level, Object obj) {
        logs.add(logger -> logger.log(level, obj));
    }

    @Override
    public synchronized void log(Level level, String msg, Throwable thrown) {
        logs.add(logger -> logger.log(level, msg, thrown));
    }

    @Override
    public synchronized void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
        logs.add(logger -> logger.log(level, msgSupplier, thrown));
    }

    @Override
    public synchronized void log(Level level, String format, Object... params) {
        logs.add(logger -> logger.log(level, format, params));
    }

    @Override
    public synchronized void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        logs.add(logger -> logger.log(level, bundle, msg, thrown));
    }

    @Override
    public synchronized void log(Level level, ResourceBundle bundle, String format, Object... params) {
        logs.add(logger -> logger.log(level, bundle, format, params));
    }

    public synchronized void logTo(System.Logger logger) {
        for (LogRecord r : logs)
            r.logTo(logger);
    }

    public synchronized void reset() {
        logs.clear();
    }

    @FunctionalInterface
    private interface LogRecord {
        void logTo(System.Logger logger);
    }
}
