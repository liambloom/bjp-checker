package dev.liambloom.checker.books.xmlBook;

import dev.liambloom.checker.books.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

public class XMLBookParser implements BookParser {
    private final AtomicBoolean optimizeForSingleChapter = new AtomicBoolean(false);

    @Override
    public Book parse(BookLocator locator) throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException, URISyntaxException {
        XMLBookReader reader = new XMLBookReader(locator.name(), locator);
        return new XMLBook(reader.getDocument(),
            new Meta(reader.getChapterType(), reader.getCheckableTypes(), reader.getClassLoader(), reader.getResources()),
            optimizeForSingleChapter.getAcquire());
    }

    @Override
    public Result<BookValidationStatus> validate(BookLocator locator) throws IOException {
        return new XMLBookReader(locator.name(), locator).validateBook();
    }

    public boolean isOptimizedForSingleChapter() {
        return optimizeForSingleChapter.getAcquire();
    }

    public void setOptimizeForSingleChapter(boolean value) {
        optimizeForSingleChapter.setRelease(value);
    }
}
