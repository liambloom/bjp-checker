package dev.liambloom.checker.ui;

import dev.liambloom.checker.books.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class SelfLoadingBook {
    // It would be more memory efficient to use a single map of SelfLoadingBooks (only one instance for each
    //  set of values), but there would be no efficient way to find in such a map, so this is the more computationally
    //  efficient method.
    private static final Map<SelfLoadingBook, FutureTask<Book>> books = Collections.synchronizedMap(new HashMap<>());
    private static final Map<SelfLoadingBook, FutureTask<Result<BookValidationStatus>>> validations = Collections.synchronizedMap(new HashMap<>());
    private final BookLocator locator;
    private final BookParser parser;
    private final FutureTask<Book> book;
    private final FutureTask<Result<BookValidationStatus>> validation;

//    private Lock bookSetLock = new ReentrantLock();

    SelfLoadingBook(String name, URL url, BookParser parser) {
        this(new BookLocator(name, url), parser);
    }

    SelfLoadingBook(BookLocator locator, BookParser parser) {
        this.locator = locator;
        this.parser = parser;
        book = books.computeIfAbsent(this, _k -> new FutureTask<>(() -> parser.getBook(locator)));
        validation = validations.computeIfAbsent(this, _k -> new FutureTask<>(() -> parser.validate(locator)));
    }

    public BookLocator locator() {
        return locator;
    }

    public BookParser url() {
        return parser;
    }

    public Book book() throws BookParserException, IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException {
        book.run();
        try {
            return book.get();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        catch (ExecutionException ee) {
            Throwable t = BookParser.reThrowError(ee.getCause());
            throw new IllegalStateException("Book parsing threw error of unexpected type" + t.getClass().getSimpleName());
        }
    }

    public Result<BookValidationStatus> validate() throws IOException {
        validation.run();
        try {
            return validation.get();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        catch (ExecutionException ee) {
            Throwable t = ee.getCause();
            if (t instanceof IOException e)
                throw e;
            else
                throw new IllegalStateException("Book parsing threw error of unexpected type" + t.getClass().getSimpleName());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(locator, parser);
    }
}
