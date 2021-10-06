module dev.liambloom.checker.books {
    uses dev.liambloom.checker.book.BookLoader;
    requires java.prefs;
    requires java.xml;
    requires dev.liambloom.checker.shared;
    exports dev.liambloom.checker.book;
}