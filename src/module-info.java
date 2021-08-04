module dev.liambloom.tests.bjp {
    requires java.xml;
    requires java.prefs;
    requires java.compiler;
    requires javafx.graphics;
    requires javafx.controls;
    requires org.fusesource.jansi;
    exports dev.liambloom.tests.bjp;
    exports dev.liambloom.tests.bjp.shared;
    exports dev.liambloom.tests.bjp.cli;
    exports dev.liambloom.tests.bjp.gui;
    //exports dev.liambloom.tests.bjp.gui to javafx.graphics;
    opens tests;
}