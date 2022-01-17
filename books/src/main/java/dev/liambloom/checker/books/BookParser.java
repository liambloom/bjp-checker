package dev.liambloom.checker.books;


import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;

public interface BookParser {
    Book parse(BookLocator locator) throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException, URISyntaxException;
    Result<BookValidationStatus> validate(BookLocator locator) throws IOException;
}
