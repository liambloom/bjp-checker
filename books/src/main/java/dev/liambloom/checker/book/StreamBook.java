package dev.liambloom.checker.book;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;

public class StreamBook extends AbstractBook {
    private InputStream stream;
    private byte[] buf = null;

    StreamBook(String name, InputStream stream) {
        super(name);
        this.stream = stream;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        if (buf == null) {
            // Is this terrible?
            buf = stream.readAllBytes();
            stream = null;
        }
        return new ByteArrayInputStream(buf);
    }

    @Override
    public boolean exists() throws IOException {
        return true;
    }

    @Override
    public void addWatcher(Consumer<WatchEvent<Path>> cb) throws IOException {

    }

    @Override
    public void removeWatcher(Consumer<WatchEvent<Path>> cb) {

    }
}
