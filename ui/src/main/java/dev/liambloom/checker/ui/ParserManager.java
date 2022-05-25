package dev.liambloom.checker.ui;

import dev.liambloom.checker.books.BookParser;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ParserManager extends ResourceManager<ParserManager.ParserRecord> {
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

    public void add(String name, URL source) throws IOException {
        add(size(), name, source);
    }

    public void add(int i, String name, URL source) throws IOException {
        try {
            inner.add(i, new ParserRecord(name, source));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public class ParserRecord extends ResourceManager<ParserRecord>.Resource {
        private BookParser parser = null;
        private Lock parserLock = new ReentrantLock();

        private ParserRecord(String name, URL sourceUrl) {
            this(validateConstructorArgs(name, sourceUrl), UUID.randomUUID(), null, sourceUrl, isNotLocalFile(sourceUrl));
        }

        private ParserRecord(String name, UUID id, Digest digest, URL sourceUrl, boolean download) {
            super(validateConstructorArgs(name, sourceUrl), id, digest, sourceUrl, download);
            addChangeListener(() -> {
                parserLock.lock();
                try {
                    parser = null;
                }
                finally {
                    parserLock.unlock();
                }
            });
        }

        public BookParser getParser() throws ResourceFileInvalidException, IOException {
            checkRemoved();
            parserLock.lock();
            try {
                if (parser == null)
                    parser = loadBookParser(getName(), getResourceUrl())
                        .orElseThrow() // TODO: Find a better way of handling this?
                        .get();
            }
            finally {
                parserLock.unlock();
            }
            return parser;
        }

        private static Optional<ServiceLoader.Provider<BookParser>> loadBookParser(String name, URL url) {
            return ServiceLoader.load(BookParser.class, new URLClassLoader(new URL[]{ url }))
                .stream()
                .filter(provider -> provider.type().getName().equals(name))
                .findAny();
        }

        private static String validateConstructorArgs(String name, URL sourceUrl) {
            if (sourceUrl.toString().endsWith("/"))
                throw new IllegalArgumentException("URL " + sourceUrl + " refers to a directory; file expected");
            else if (loadBookParser(name, sourceUrl).isEmpty())
                throw new IllegalArgumentException("JAR " + sourceUrl + " did not provide a BookParser named " + name);
            else
                return name;
        }

        @Override
        public void setSourceUrl(URL value) throws IOException {
            validateConstructorArgs(getName(), value);
            super.setSourceUrl(value);
        }

        // TODO: invalidation stuff
    }
}
