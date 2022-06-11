package dev.liambloom.checker.ui;

import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;

public final class Data {
    private Data() {}

    private static BookManager books;
    private static ParserManager parsers;
    private static UserConfig userConfig;

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

    public static UserConfig userConfig() {
        if (userConfig == null)
            throw new IllegalStateException("Data is not yet initialized");
        return userConfig;
    }

    public static synchronized void initialize() throws IOException, TransformerException, SAXException, IncompatibleSettingsVersionException {
        if (books != null)
            throw new IllegalStateException("Data already initialized");
        books = new BookManager();
        parsers = new ParserManager();
        userConfig = new UserConfig();
    }
}
