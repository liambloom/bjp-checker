package dev.liambloom.tests.book.bjp3;

abstract class Option<T> {
    private final T value;

    protected Option(T value) {
        this.value = value;
    }

    public abstract String name();
    public abstract String help();
    public T value() {
        return value;
    }
    public String[] flags() {
        return new String[]{ "-" + name().charAt(0), "--" + name() };
    }
    // Class<T> type();
}
