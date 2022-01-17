module dev.liambloom.checker.internal {
    requires java.xml;
    requires java.prefs;
    requires javafx.graphics;
//    requires dev.liambloom.checker.books;
    requires dev.liambloom.util.function;
    requires dev.liambloom.util.base;
    requires dev.liambloom.checker.books;
//    requires org.fusesource.jansi;
//    opens tests;
    exports dev.liambloom.checker;
    exports dev.liambloom.checker.internal to dev.liambloom.checker.ui;
}