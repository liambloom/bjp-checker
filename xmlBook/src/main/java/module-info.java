module dev.liambloom.checker.books.xmlBook {
    exports dev.liambloom.checker.books.xmlBook;
    requires dev.liambloom.checker.books;
    requires dev.liambloom.util.function;
    requires dev.liambloom.util.base;
    requires com.google.auto.service;
    requires java.xml;
    provides dev.liambloom.checker.books.BookParser with dev.liambloom.checker.books.xmlBook.XMLBookParser;
}