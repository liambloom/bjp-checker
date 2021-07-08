module tests.bjp {
    requires org.fusesource.jansi;
    requires java.xml;
    requires javafx.graphics;
    requires javafx.controls;
    //requires org.eclipse.wst.xml.xpath2.processor;
    exports dev.liambloom.tests.bjp;
    exports dev.liambloom.tests.bjp.checker to javafx.graphics;
}