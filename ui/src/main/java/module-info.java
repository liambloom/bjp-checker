module dev.liambloom.checker.ui {
    requires static java.xml;
    requires java.prefs;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires dev.liambloom.checker.internal;
    requires dev.liambloom.checker.books;
    requires org.fusesource.jansi;
    requires dev.liambloom.checker.shared;
    exports dev.liambloom.checker.cli;
    exports dev.liambloom.checker.gui;
    opens views;
    opens css;
    opens dev.liambloom.checker.gui to javafx.fxml;
}