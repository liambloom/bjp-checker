package dev.liambloom.checker.books.xmlBook;

import com.google.auto.service.AutoService;
import dev.liambloom.checker.books.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoService(BookParser.class)
public class XMLBookParser extends BookParser {
    private final AtomicBoolean optimizeForSingleChapter = new AtomicBoolean(false);

    @Override
    public Book parse(BookLocator locator) throws IOException, ClassNotFoundException, NoSuchMethodException, URISyntaxException, BookParserException {
        XMLBookReader reader = new XMLBookReader(locator.name(), locator);
        try {
            return new XMLBook(locator,
                reader.getDocument(),
                new Meta(reader.getChapterType(), reader.getCheckableTypes(), reader.getClassLoader(), reader.getResources()),
                optimizeForSingleChapter.getAcquire());
        }
        catch (SAXException e) {
            throw new BookParserException(e);
        }
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
