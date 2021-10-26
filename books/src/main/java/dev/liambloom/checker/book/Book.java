package dev.liambloom.checker.book;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface Book {
    /*
     * Validates the test using the schema and also checking
     * to make sure that all types referenced in the book are valid. The status of the
     * returned result will be an instance of {@link TestValidationStatus}
     *
     * @return A {@code Result} representing the result of the validation
     * @throws IOException If an i/o error occurs
     * @see AbstractBook#getSchema()
     *
    Result<TestValidationStatus> validate() throws IOException;

    /*
     * Returns the document represented by this book. An exception will be thrown if
     * the document is not valid.
     *
     * @return The document represented by this book
     * @throws IOException            If an i/o error occurs
     * @throws SAXException           If the document is not valid XML or does not follow the schema
     * @throws ClassNotFoundException If the document references a class that does not exist
     * @throws IllegalStateException  If the book does not exist
     * @throws UnsupportedOperationException If using this document requires operations that are not supported (such as file resolution)
     * @see AbstractBook#getSchema()
     * @see #validate()
     * @see #exists()
     *
    Document getDocument() throws IOException, SAXException, ClassNotFoundException;*/

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
    default Path loadResources(Path destination, Stream<String> resources) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to check if this book supports path resolution.
     * 
     * @return {@code true} if this book supports path resolution, {@code false} otherwise
     * @see #loadResources(Path, Stream)
     */
    default boolean supportsResourceLoading() {
        return false;
    }

    /**
     * Adds a watcher that watches for changes in this book. Multiple instances of the
     * same callback can be added, and if they are, the callback will be called once for
     * each time it was added.
     *
     * @param cb The function to call when a change is detected
     * @throws IOException If an i/o error occurs
     */
    void addWatcher(Consumer<WatchEvent<?>> cb) throws IOException, URISyntaxException;

    /**
     * Removes a watcher from the book. If there are multiple instances of the same
     * callback, it decrements the number of times that the callback will be called.
     * If there is no instances of the given callback, it returns {@code false}
     *
     * @param cb The callback of the watcher to remove.
     * @return whether the callback was removed
     */
    boolean removeWatcher(Consumer<WatchEvent<?>> cb) throws IOException, URISyntaxException;
}
