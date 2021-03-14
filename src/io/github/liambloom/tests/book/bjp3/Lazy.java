package io.github.liambloom.tests.book.bjp3;

abstract class Lazy<T> {
    private T value;
    // This should probably be atomic
    private boolean isInitialized = false;

    protected abstract T createValue();

    public final T get() {
        if (!isInitialized) {
            value = createValue();
            isInitialized = true;
        }
        return value;
    }
}
