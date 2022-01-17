package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.Book;
import dev.liambloom.checker.books.Chapter;
import dev.liambloom.checker.books.Meta;
import org.w3c.dom.Document;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class XMLBook implements Book {
    private final Document document;
    private final Meta meta;

    XMLBook(Document document, Meta meta) {
        this.document = document;
        this.meta = meta;
    }

    @Override
    public Chapter getChapter(int chapter) {

    }

    @Override
    public Meta getMeta() {
        return meta;
    }
}
