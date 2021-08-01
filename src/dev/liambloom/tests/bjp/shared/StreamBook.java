package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

public class StreamBook extends Book {
    private final InputStream stream;
    private final Source source;

    StreamBook(String name, InputStream stream) {
        super(name);
        this.stream = stream;
        this.source = new StreamSource(stream);
    }

    @Override
    public Document getDocument(DocumentBuilder db) throws SAXException, IOException {
        return db.parse(stream);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected Source getSource() {
        return source;
    }
}
