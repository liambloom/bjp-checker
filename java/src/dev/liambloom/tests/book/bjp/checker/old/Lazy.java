package dev.liambloom.tests.book.bjp.checker.old;

abstract class Lazy<T> {
    private T value;
    // This should probably be atomic
    private boolean isInitialized = false;

    protected abstract T createValue();

    public final synchronized T get() {
        if (!isInitialized) {
            value = createValue();
            isInitialized = true;
        }
        return value;
    }
}
