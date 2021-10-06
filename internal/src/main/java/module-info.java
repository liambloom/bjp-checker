module dev.liambloom.checker.internal {
    requires java.xml;
    requires java.prefs;
    requires javafx.graphics;
//    requires dev.liambloom.checker.bjp.annotations;
    requires dev.liambloom.checker.books;
    requires dev.liambloom.checker.shared;
    exports dev.liambloom.checker.internal;
    opens tests;
}