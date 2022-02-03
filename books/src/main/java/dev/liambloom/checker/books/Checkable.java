package dev.liambloom.checker.books;

public interface Checkable {
    Test[] tests(StaticExecutableTest.Factory factory);
    String getName();
    Chapter getChapter();
}
