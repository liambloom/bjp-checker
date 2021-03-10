package io.github.liambloom.tests.book.bjp3;

import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;

public class TestLoader {
    protected final static Schema schema;

    static {
        Schema s;
        try {
            // FIXME: It's not finding the xerces xsd 1.1 parser
            s = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(new StreamSource(TestLoader.class.getResourceAsStream("../book-tests.xsd")));
        } catch (SAXException e) {
            e.printStackTrace();
            s = null;
            Main.debugger.error("Failed to load test schema");
        }
        schema = s;
        System.out.println("Schema loaded successfully");
    }

    public static void load() {}
}
