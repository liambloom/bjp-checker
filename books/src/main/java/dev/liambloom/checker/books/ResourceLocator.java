package dev.liambloom.checker.books;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public interface ResourceLocator {
    /**
     * Returns the input stream represented by this book.
     *
     * @return The input stream
     * @throws IOException If an i/o error occurs
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns {@code true} if the document "exists". The exact definition of this may
     * vary between implementations. The default implementation returns true if, and
     * only if, {@code Books.bookNameExists(this.getName())} returns {@code true}.
     *
     * @return True if the book exists, false otherwise
     * @throws IOException If an i/o error occurs
     */
    boolean exists() throws IOException;

    URI getURI() throws URISyntaxException;
}
