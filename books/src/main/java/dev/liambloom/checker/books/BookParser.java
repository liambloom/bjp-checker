package dev.liambloom.checker.books;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BookParser {
    public final Book getBook(BookLocator locator) throws IOException, ClassNotFoundException, NoSuchMethodException, URISyntaxException, BookParserException {
        Book book = parse(locator);

        if (book.getLocator() != locator)
            throw new IllegalStateException("Book created with incorrect locator");

        if (book.getMeta().resources().length > 0)
            throw new IllegalStateException("Resources are not currently supported");
        /* TODO This needs to somehow be checked in the validate method
        String base = book.getMeta().resourceBase().normalize().getPath();
        if (!base.endsWith("/"))
            base += "/";
        URI bookUri = book.getLocator().url().toURI().normalize();
        URI absoluteBase = bookUri.resolve(base).normalize();

//        String bookPath = book.getLocator().url().toURI().normalize().getPath();
//        if (!bookPath.startsWith(absoluteBase))
//            throw new IllegalArgumentException("Book is not located in resources");

        for (URI uri : book.getMeta().resources()) {
            // TODO:
            // -[ ] get the resolved uri for the resource and then turn it into a url and open a connection for the input stream
            // -[ ] Also resource base should possibly be in meta rather than the locator?
            // -[x] This should be checked by BookParser somehow
            // -[ ] Check to make sure resource path does not have a relative path that goes outside of
            //          base at ANY time (e.g. "../foo" or "/path/to/base/foo"). Keep in mind that URI#resolve
            //          will normalize relative paths
            String resourcePath = book.getLocator().url().toURI().resolve(uri).normalize().getPath();
            if (!resourcePath.startsWith(base))
                throw new IllegalArgumentException("Resource " + resourcePath + " not located below resource base " + base);


            for (String segment : uri.getPath().split("/")) {

            }
        }*/

        return book;
    }
    protected abstract Book parse(BookLocator locator) throws IOException, ClassNotFoundException, NoSuchMethodException, URISyntaxException, BookParserException;
    public abstract Result<BookValidationStatus> validate(BookLocator locator) throws IOException;

    /**
     * A convenience method to re-throw an error thrown by a book parser and stored, such
     * as in a variable or a container exception
     *
     * @param throwable The exception to be thrown
     * @return The exception, if it is not of one of the thrown types
     * @throws RuntimeException If {@code throwable} is a {@code RuntimeException}
     * @throws IOException If {@code throwable} is an {@code IOException}
     * @throws ClassNotFoundException If {@code throwable} is a {@code ClassNotFoundException}
     * @throws NoSuchMethodException If {@code throwable} is a {@code NoSuchMethodException}
     * @throws BookParserException If {@code throwable} is a {@code BookParserException}
     */
    public static Throwable reThrowError(Throwable throwable) throws IOException, ClassNotFoundException, NoSuchMethodException, BookParserException, URISyntaxException {
        if (throwable instanceof RuntimeException e)
            throw e;
        else if (throwable instanceof IOException e)
            throw e;
        else if (throwable instanceof BookParserException e)
            throw e;
        else if (throwable instanceof ClassNotFoundException e)
            throw e;
        else if (throwable instanceof NoSuchMethodException e)
            throw e;
        else if (throwable instanceof URISyntaxException e)
            throw e;
        else
            return throwable;
    }
}
