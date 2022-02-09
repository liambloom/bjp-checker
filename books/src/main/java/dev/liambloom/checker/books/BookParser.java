package dev.liambloom.checker.books;

import java.io.IOException;
import java.net.URISyntaxException;

public interface BookParser {
    Book parse(BookLocator locator) throws IOException, ClassNotFoundException, NoSuchMethodException, URISyntaxException, BookParserException;
    Result<BookValidationStatus> validate(BookLocator locator) throws IOException;
}
