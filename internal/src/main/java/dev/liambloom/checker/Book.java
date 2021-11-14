package dev.liambloom.checker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class Book {
    /**
     * Returns the input stream represented by this book.
     *
     * @return The input stream
     * @throws IOException If an i/o error occurs
     */
    protected abstract InputStream getInputStream() throws IOException;

    /**
     * Returns {@code true} if the document "exists". The exact definition of this may
     * vary between implementations. The default implementation returns true if, and
     * only if, {@code Books.bookNameExists(this.getName())} returns {@code true}.
     *
     * @return True if the book exists, false otherwise
     * @throws IOException If an i/o error occurs
     */
    public abstract boolean exists() throws IOException;

    /**
     * Loads all the resources required by a book. The list of resources is passed in by a
     * {@code dev.liambloom.checker.BookReader} (not found in this module). The resources
     * should be loaded and placed into the destination directory.
     *
     * This function returns the location that the resources were loaded relative to. For
     * example: if {@code resources} contains the path {@code ../foo.txt}, then this should
     * <strong>not</strong> be placed in the parent directory of {@code destination}. Instead,
     * it should be placed directly into {@code destination}, and the function should return a
     * directory one level deeper than {@code destination}. This way, provided relative path
     * relative to the return value points to the correct file.
     *
     * Also note: the return value should <em>never</em> be outside of {@code destination}.
     *
     * If in {@code resources} contains a path to a resource that does not exist, simply skip
     * it, rather than throwing a {@code FileNotFoundException}, as this may be intentional.
     *
     * The default implementation throws an {@link UnsupportedOperationException}. If you do
     * not override this method to provide an implementation, then validation MUST fail if
     * the {@code <File>} element appears at any point in the document.
     *
     * @param destination The folder in which to place the loaded resources
     * @param resources The resources which must be loaded
     * @return The root path from which to access the resources with the given paths.
     * @throws IOException If an i/o error occurs
     * @throws UnsupportedOperationException If this book does not support path resolution.
     */
    protected Path loadResources(Path destination, String[] resources) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to check if this book supports path resolution.
     * 
     * @return {@code true} if this book supports path resolution, {@code false} otherwise
     * @see #loadResources(Path, Stream)
     */
    protected boolean supportsResourceLoading() {
        return false;
    }

    public final BookReader getReader() {
        return new BookReader(this);
    }
}
