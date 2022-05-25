package dev.liambloom.checker.ui;

import dev.liambloom.checker.books.*;
import dev.liambloom.checker.ui.cli.UserErrorException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BookManager extends ResourceManager<BookManager.SelfLoadingBook> {
    BookManager() throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
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
            Data.parsers().get(UUID.fromString(e.getElementsByTagName("parser").item(0).getTextContent()))
        );
    }

    public void add(String name, URL source, ParserManager.ParserRecord parser) throws IOException {
        add(size(), name, source, parser);
    }

    public void add(int i, String name, URL source, ParserManager.ParserRecord parser) throws IOException {
        try {
            inner.add(i, new SelfLoadingBook(name, source, parser));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public class SelfLoadingBook extends ResourceManager<SelfLoadingBook>.Resource {
        private ParserManager.ParserRecord parser;
        private Book book = null;
        private final Lock bookLock = new ReentrantLock();
        private Result<BookValidationStatus> validation = null;
        private final Lock validationLock = new ReentrantLock();


        private SelfLoadingBook(BookLocator locator, ParserManager.ParserRecord parser) {
            this(locator.name(), locator.url(), parser);
        }

        private SelfLoadingBook(String name, URL sourceUrl, ParserManager.ParserRecord parser) {
            this(name, UUID.randomUUID(), null, sourceUrl, isNotLocalFile(sourceUrl), parser);
        }

        private SelfLoadingBook(String name, UUID id, Digest digest, URL sourceUrl, boolean download, ParserManager.ParserRecord parser) {
            super(name, id, digest, sourceUrl, download);
            this.parser = parser;
            addChangeListener(() -> {
                Thread t = new Thread(() -> {
                    validationLock.lock();
                    try {
                        validation = null;
                    }
                    finally {
                        validationLock.unlock();;
                    }
                });
                t.start();
                bookLock.lock();
                try {
                    book = null;
                }
                finally {
                    bookLock.unlock();
                }
                try {
                    t.join();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            if (parser != null)
                addParserListeners();
        }

        private void onParserRemoved() {
            this.parser = null;
            this.changed();
        }

        private void addParserListeners() {
            parser.addChangeListener(this::changed);
            parser.addRemovalListener(this::onParserRemoved);
        }

        @Override
        public void setName(String value) {
            super.setName(value);
        }

        public BookLocator getLocator() throws ResourceFileInvalidException, IOException {
            checkRemoved();
            return new BookLocator(getName(), getResourceUrl());
        }

        public Optional<ParserManager.ParserRecord> getParser() {
            checkRemoved();
            return Optional.ofNullable(parser);
        }

        public void setParser(ParserManager.ParserRecord value) {
            checkRemoved();
            Objects.requireNonNull(value);
            parser.removeChangeListener(this::changed);
            parser.removeRemovalListener(this::onParserRemoved);
            parser = value;
            addParserListeners();
            changed();
        }

        public Book book() throws BookParserException, IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, ResourceFileInvalidException {
            checkRemoved();
            bookLock.lock();
            try {
                if (book == null)
                    book = getParser().orElseThrow().getParser().getBook(getLocator());
            }
            finally {
                bookLock.unlock();
            }
            return book;
        }

        public Result<BookValidationStatus> validate() throws IOException, ResourceFileInvalidException {
            checkRemoved();
            validationLock.lock();
            try {
                if (validation == null)
                    validation = getParser().orElseThrow().getParser().validate(getLocator());
            }
            finally {
                validationLock.unlock();
            }
            return validation;
        }
    }
}
