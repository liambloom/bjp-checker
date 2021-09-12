module dev.liambloom.checker.bjp.ui {
    requires static java.xml;
    requires java.prefs;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires dev.liambloom.checker.bjp.api;
    requires org.fusesource.jansi;
    exports dev.liambloom.checker.bjp.cli;
    exports dev.liambloom.checker.bjp.gui;
    opens views;
    opens css;
    opens dev.liambloom.checker.bjp.gui to javafx.fxml;
}