package dev.liambloom.tests.book.bjp.checker;

public final class Tuple2<T, U> {
    private T e0;
    private U e1;

    public Tuple2(T e0, U e1) {
        this.e0 = e0;
        this.e1 = e1;
    }

    public T e0() {
        return e0;
    }

    public U e1() {
        return e1;
    }
}
