package dev.liambloom.checker.ui;

import dev.liambloom.checker.books.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
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
            parser.setTextContent(selfLoadingBook.getParser().map(ParserManager.ParserRecord::getId).orElseGet(UUID::randomUUID).toString());
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

        public void add(String name, URL source, ParserManager.ParserRecord parser) {
            add(size(), name, source, parser);
        }

        public void add(int i, String name, URL source, ParserManager.ParserRecord parser) {
            addResource(i, new SelfLoadingBook(name, source, parser));
        }

        public class SelfLoadingBook extends ResourceManager<SelfLoadingBook>.Resource {
            private ParserManager.ParserRecord parser;
            private final AtomicReference<Book> book = new AtomicReference<>();
            private final AtomicReference<Result<BookValidationStatus>> validation = new AtomicReference<>();


            private SelfLoadingBook(BookLocator locator, ParserManager.ParserRecord parser) {
                this(locator.name(), locator.url(), parser);
            }

            private SelfLoadingBook(String name, URL sourceUrl, ParserManager.ParserRecord parser) {
                this(name, UUID.randomUUID(), null, sourceUrl, isNotLocalFile(sourceUrl), parser);
            }

            private SelfLoadingBook(String name, UUID id, Digest digest, URL sourceUrl, boolean download, ParserManager.ParserRecord parser) {
                super(name, id, digest, sourceUrl, download);
                this.parser = parser;
                addListener(() -> {
                    Thread t = new Thread(() -> validation.set(null) );
                    t.start();
                    book.set(null);
                    try {
                        t.join();
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                if (parser != null)
                    parser.addListener(this::changed);
            }

            @Override
            public void setName(String value) {
                super.setName(value);
            }

            @Override
            public void setSourceUrl(URL value) throws IOException {
                super.setSourceUrl(value);
            }

            public BookLocator getLocator() throws ResourceFileInvalidException, IOException {
                return new BookLocator(getName(), getResourceUrl());
            }

            public Optional<ParserManager.ParserRecord> getParser() {
                return Optional.ofNullable(parser);
            }

            public void setParser(ParserManager.ParserRecord value) {
                Objects.requireNonNull(value);
                parser.removeListener(this::changed);
                parser = value;
                parser.addListener(this::changed);
                changed();
            }

            public Book book() throws BookParserException, IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, ResourceFileInvalidException {
                return book.compareAndExchange(null, getParser().orElseThrow().getParser().getBook(getLocator()));
            }

            public Result<BookValidationStatus> validate() throws IOException, ResourceFileInvalidException {
                return validation.compareAndExchange(null, getParser().orElseThrow().getParser().validate(getLocator()));
            }
        }
    }

    public static class ParserManager extends ResourceManager<ParserManager.ParserRecord> {
        ParserManager() throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
            super("parser");
        }

        @Override
        protected ParserRecord parseElement(BaseResourceData parsed, Element e) {
            return new ParserRecord(
                parsed.name(),
                parsed.id(),
                parsed.digest(),
                parsed.sourceUrl(),
                parsed.download()
            );
        }

        public class ParserRecord extends ResourceManager<ParserRecord>.Resource {
            private final AtomicReference<BookParser> parser = new AtomicReference<>();

            private ParserRecord(String name, URL sourceUrl) {
                this(validateConstructorArgs(name, sourceUrl), UUID.randomUUID(), null, sourceUrl, isNotLocalFile(sourceUrl));
            }

            private ParserRecord(String name, UUID id, Digest digest, URL sourceUrl, boolean download) {
                super(name, id, digest, sourceUrl, download);
                addListener(() -> parser.set(null));
            }

            public BookParser getParser() throws ResourceFileInvalidException, IOException {
                return parser.compareAndExchange(null, loadBookParser(getName(), getResourceUrl())
                    .orElseThrow() // TODO: Find a better way of handling this?
                    .get());
            }

            private static Optional<ServiceLoader.Provider<BookParser>> loadBookParser(String name, URL url) {
                return ServiceLoader.load(BookParser.class, new URLClassLoader(new URL[]{url}))
                    .stream()
                    .filter(provider -> provider.type().getName().equals(name))
                    .findAny();
            }

            private static String validateConstructorArgs(String name, URL sourceUrl) {
                if (sourceUrl.toString().endsWith("/"))
                    throw new IllegalArgumentException("URL " + sourceUrl + " refers to a directory; file expected");
                else if (loadBookParser(name, sourceUrl).isEmpty())
                    throw new IllegalArgumentException("JAR " + sourceUrl + " did not provide a BookLoader named " + name);
                else
                    return name;
            }

            // TODO: invalidation stuff
        }
    }
}
