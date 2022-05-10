package dev.liambloom.checker.ui;

import dev.liambloom.checker.books.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

public final class Data {
    private Data() {}

    private static BookManager books;
    private static ParserManager parsers;

    public static BookManager books() {
        if (books == null)
            throw new IllegalStateException("Data not yet initialized");
        return books;
    }

    public static ParserManager parsers() {
        if (parsers == null)
            throw new IllegalStateException("Data not yet initialized");
        return parsers;
    }

    public static synchronized void initialize() throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
        if (books != null)
            throw new IllegalStateException("Data already initialized");
        books = new BookManager();
        parsers = new ParserManager();
    }

    public static class BookManager extends ResourceManager<BookManager.SelfLoadingBook> {
        private BookManager() throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
            super("book");
        }

        @Override
        protected Element getElement(SelfLoadingBook selfLoadingBook) {
            Element e = super.getElement(selfLoadingBook);
            Element parser = document.createElement("parser");
            parser.setTextContent(selfLoadingBook.getParser().getId().toString());
            e.appendChild(parser);
            return e;
        }

        @Override
        protected SelfLoadingBook parseElement(BaseResourceData parsed, Element e) {
            return new SelfLoadingBook(
                parsed.name(),
                parsed.id(),
                parsed.digest(),
                parsed.sourceUrl(),
                parsed.download(),
                parsers().get(UUID.fromString(e.getElementsByTagName("parser").item(0).getTextContent()))
            );
        }

        public class SelfLoadingBook extends BookManager.Resource {
            private ParserManager.ParserRecord parser;
            private final AtomicReference<Book> book = new AtomicReference<>();
            private final AtomicReference<Result<BookValidationStatus>> validation = new AtomicReference<>();


            private SelfLoadingBook(BookLocator locator, ParserManager.ParserRecord parser) {
                this(locator.name(), locator.url(), parser);
            }

            private SelfLoadingBook(String name, URL url, ParserManager.ParserRecord parser) {
                super(name, url);
                this.parser = parser;
                // Problem: Update isn't going to work. Neither are any future setters, such as `rename`
                //  On the bright side, since I'm not using JFX with all of its complicated whatevers,
                //  I can simply override setters (which I should make protected in ResourceManager.Resource)
                //  rather than relying on some super complex listeners and stuff
//                book = new FutureTask<>(() -> parser.getBook(getLocator()));
//                validation = new FutureTask<>(() -> parser.validate(getLocator()));
//
//                book.runAndReset();
            }

            private SelfLoadingBook(String name, UUID id, Digest digest, URL sourceUrl, boolean download, ParserManager.ParserRecord parser) {
                super(name, id, digest, sourceUrl, download);

                // Repeated. I don't like it, but I don't think there's much I can do
                this.parser = parser;
//                book = new FutureTask<>(() -> parser.getBook(getLocator()));
//                validation = new FutureTask<>(() -> parser.validate(getLocator()));
            }

            public BookLocator getLocator() {
                return new BookLocator(getName(), getSourceUrl());
            }

            public ParserManager.ParserRecord getParser() {
                return parser;
            }

            public void setParser(ParserManager.ParserRecord value) {
                parser = value;
                resetParsing();
            }

            @Override
            public void setName(String value) {
                super.setName(value);
                resetParsing();
            }

            @Override
            public void setSourceUrl(URL value) throws IOException {
                super.setSourceUrl(value);
                resetParsing();
            }

            private void resetParsing() {
                Thread t = new Thread(() -> validation.set(null) );
                t.start();
                book.set(null);
                try {
                    t.join();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            public Book book() throws BookParserException, IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException {
                return book.compareAndExchange(null, getParser().getParser().getBook(getLocator()));
            }

            public Result<BookValidationStatus> validate() throws IOException {
                return validation.compareAndExchange(null, getParser().getParser().validate(getLocator()));
            }
        }
    }

    public static class ParserManager extends ResourceManager<ParserManager.ParserRecord> {
        ParserManager() throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
            super("parser");
        }

        @Override
        protected Element getElement(ParserRecord selfLoadingBook) {
            return ;
        }

        @Override
        protected ParserRecord parseElement(BaseResourceData parsed, Element e) {
            return ;
        }

        public class ParserRecord extends ParserManager.Resource {
            private ParserRecord(String name, URL sourceUrl) { // Parser files MUST BE JARS
                super(name, sourceUrl);
            }

            private ParserRecord(String name, UUID id, Digest digest, URL sourceUrl, boolean download) {
                super(name, id, digest, sourceUrl, download);
            }

            public BookParser getParser() {
                return ;
            }
        }
    }
}
