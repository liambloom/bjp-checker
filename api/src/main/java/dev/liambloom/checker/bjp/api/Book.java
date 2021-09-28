package dev.liambloom.checker.bjp.api;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;

public interface Book {
    /**
     * Returns the name of the document
     *
     * @return The name of the document
     */
    String getName();

    /**
     * Validates the test using the schema and also checking
     * to make sure that all types referenced in the book are valid. The status of the
     * returned result will be an instance of {@link TestValidationStatus}
     *
     * @return A {@code Result} representing the result of the validation
     * @throws IOException If an i/o error occurs
     * @see Books#getSchema()
     */
    Result<TestValidationStatus> validate() throws IOException;

    /**
     * Returns the document represented by this book. An exception will be thrown if
     * the document is not valid.
     *
     * @return The document represented by this book
     * @throws IOException            If an i/o error occurs
     * @throws SAXException           If the document is not valid XML or does not follow the schema
     * @throws ClassNotFoundException If the document references a class that does not exist
     * @throws IllegalStateException  If the book does not exist
     * @throws UnsupportedOperationException If using this document requires operations that are not supported (such as file resolution)
     * @see Books#getSchema()
     * @see #validate()
     * @see #exists()
     */
    Document getDocument() throws IOException, SAXException, ClassNotFoundException;

    /**
     * Returns {@code true} if the document "exists". The exact definition of this may
     * vary between implementations. The default implementation returns true if, and
     * only if, {@code Books.bookNameExists(this.getName())} returns {@code true}.
     *
     * @return True if the book exists, false otherwise
     * @throws IOException If an i/o exception occurs
     */
    default boolean exists() throws IOException {
        return Books.bookNameExists(getName());
    }

    /**
     * Resolves a path, which may be a relative path (in which case it should be resolved
     * relative to the book), or may be an absolute path, in which case it should be relative
     * to the root.
     *
     * The default implementation throws an {@link UnsupportedOperationException}. If you do
     * not override this method to provide an implementation, then validation MUST fail if
     * the {@code <File>} element appears at any point in the document.
     *
     * @param p The path to resolve
     * @return The path represented by the argument (does not have to exist)
     * @throws IOException If an i/o error occurs
     * @throws UnsupportedOperationException If this book does not support path resolution.
     */
    default Path resolve(Path p) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Used to check if this book supports path resolution.
     * 
     * @return {@code true} if this book supports path resolution, {@code false} otherwise
     * @see #resolve(Path)
     */
    default boolean supportsFileResolution() {
        return false;
    }
}
