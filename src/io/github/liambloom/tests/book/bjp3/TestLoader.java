package io.github.liambloom.tests.book.bjp3;

import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class TestLoader {
    protected static final Schema schema;

    static {
        Schema s;
        try {
            // FIXME: book-tests.xsd might not be ending up in the jar
            InputStream stream = TestLoader.class.getResourceAsStream("../../../../../../book-tests.xsd");
            /* System.out.write(stream.readAllBytes());
            System.out.println(); */
            s = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(new StreamSource(stream));
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
