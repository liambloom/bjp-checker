module dev.liambloom.checker.bjp.api {
    requires java.xml;
    requires java.prefs;
    requires javafx.graphics;
    exports dev.liambloom.checker.bjp;
    exports dev.liambloom.checker.bjp.api;
    opens tests;
}