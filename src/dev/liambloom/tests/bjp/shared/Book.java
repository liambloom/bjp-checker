package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.cli.CLILogger;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.*;

public interface Book {
    Result validate() throws IOException;
    String getName();
    default boolean exists() throws IOException {
        return Books.bookNameExists(getName());
    }
    default Document getDocument() throws IOException, SAXException {
        DocumentBuilder db = Books.getDocumentBuilderPool().get();
        Document r = getDocument(db);
        Books.getDocumentBuilderPool().offer(db);
        return r;
    }
    Document getDocument(DocumentBuilder db) throws SAXException, IOException;
}
