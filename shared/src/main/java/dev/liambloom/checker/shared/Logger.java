package dev.liambloom.checker.shared;

public interface Logger {
    Logger logger = new WrapperLogger((logKind, msg, args) -> {
        if (logKind == LogKind.ERROR)
            throw new RuntimeException(String.format(msg, args));
    });

    void log(LogKind logKind, String msg, Object... args);

    static void setLogger(Logger logger) {
        ((WrapperLogger) Logger.logger).inner = logger;
    }
}

class WrapperLogger implements Logger {
    Logger inner;

    public WrapperLogger(Logger inner) {
        this.inner = inner;
    }

    @Override
    public void log(LogKind logKind, String msg, Object... args) {
        inner.log(logKind, msg, args);
    }
}