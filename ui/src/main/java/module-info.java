import dev.liambloom.checker.ui.CheckerUILoggerFinder;

module dev.liambloom.checker.ui {
    uses System.LoggerFinder;
    requires static java.xml; // why is this static?
    requires java.prefs;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires dev.liambloom.checker.internal;
    requires com.google.auto.service;
//    requires dev.liambloom.checker.books;
    requires org.fusesource.jansi;
    requires dev.liambloom.util.function;
    requires dev.liambloom.util.base;
    // requires net.harawata.appdirs;
    exports dev.liambloom.checker.ui.cli;
    exports dev.liambloom.checker.ui.gui;
    exports dev.liambloom.checker.ui;
    provides System.LoggerFinder with dev.liambloom.checker.ui.CheckerUILoggerFinder;
    opens views;
    opens css;
    opens dev.liambloom.checker.ui.gui to javafx.fxml;
    opens dev.liambloom.checker.ui to javafx.fxml;
}