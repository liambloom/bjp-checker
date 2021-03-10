package io.github.liambloom.tests.book.bjp3;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

public class Test {
    public static void main(String[] args) throws SAXException {
        StreamSource[] schemaDocuments = new StreamSource[0];/* created by your application */;
        //Source instanceDocument = /* created by your application */;

        SchemaFactory sf = SchemaFactory.newInstance(
                "http://www.w3.org/XML/XMLSchema/v1.1");
        //Schema s = sf.newSchema(schemaDocuments);
        //Validator v = s.newValidator();
        //v.validate(instanceDocument);
    }
}
