module dev.liambloom.checker.bjp.api {
    requires java.xml;
    requires java.prefs;
    requires javafx.graphics;
    requires dev.liambloom.checker.bjp.annotations;
    exports dev.liambloom.checker.bjp.api;
    opens tests;
}