package dev.liambloom.checker.books;

public interface Post {
    Result<TestStatus> check(Object object);
}
