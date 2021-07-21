module dev.liambloom.tests.bjp {
    requires java.xml;
    requires java.prefs;
    requires java.compiler;
    requires javafx.graphics;
    requires javafx.controls;
    requires org.fusesource.jansi;
    exports dev.liambloom.tests.bjp;
    exports dev.liambloom.tests.bjp.checker to javafx.graphics;
}