package dev.liambloom.checker.books;

public abstract class Book {
    private final Meta meta;
    private final BookLocator locator;

    public Book(Meta meta, BookLocator locator) {
        this.meta = meta;
        this.locator = locator;
    }

    public abstract Chapter getChapter(int chapter);

    public Meta getMeta() {
        return meta;
    }

    public BookLocator getLocator() {
        return locator;
    }
}
