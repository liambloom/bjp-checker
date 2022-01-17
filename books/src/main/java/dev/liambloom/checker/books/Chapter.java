package dev.liambloom.checker.books;

public interface Chapter {
    Checkable getCheckable(CheckableType<?> type, int number);
}
