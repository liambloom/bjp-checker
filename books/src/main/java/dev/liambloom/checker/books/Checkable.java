package dev.liambloom.checker.books;

public interface Checkable {
    StaticExecutableTestInfo[] tests();
    String getName();
    Chapter getChapter();
}
