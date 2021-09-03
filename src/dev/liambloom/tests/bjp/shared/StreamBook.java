package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

public class StreamBook extends AbstractBook {
    private final InputStream stream;

    StreamBook(String name, InputStream stream) {
        super(name);
        this.stream = stream;
    }

    @Override
    protected InputStream getInputStream() {
        return stream;
    }
}
