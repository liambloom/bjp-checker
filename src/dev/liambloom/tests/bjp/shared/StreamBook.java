package dev.liambloom.tests.bjp.shared;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
}
