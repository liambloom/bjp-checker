module dev.liambloom.checker.books {
    uses dev.liambloom.checker.book.URLWatcher;
    requires dev.liambloom.util.function;
//    requires dev.liambloom.util.base;
    provides dev.liambloom.checker.book.URLWatcher with dev.liambloom.checker.book.FileURLWatcher
        , dev.liambloom.checker.book.JarURLWatcher;
    exports dev.liambloom.checker.book;
}