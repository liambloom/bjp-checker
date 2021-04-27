package dev.liambloom.tests.book.bjp.checker;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import static org.fusesource.jansi.Ansi.Color;

public class TestValidationResult extends Result {
    public static final Pattern DOT_AT_END = Pattern.compile("\\.?$");

    public final byte[] message;

    public TestValidationResult(File file, Variant variant) throws IOException {
        this(file, variant, null);
    }

    public TestValidationResult(File file, Variant variant, SAXException error) throws IOException {
        super(file.getName().substring(0, file.getName().lastIndexOf('.')), variant);

        if (error == null) {
            message = null;
            return;
        }

        String message = error.getMessage();
        if (message.endsWith("."))
            message = message.substring(0, message.length() - 1);

        if (error instanceof SAXParseException) {
            SAXParseException parseException = (SAXParseException) error;
            message += String.format(" at %s:%d:%d", file.getCanonicalPath(), parseException.getLineNumber(), parseException.getColumnNumber());
        }
        this.message = message.getBytes();
    }

    @Override
    public void printToStream(OutputStream out) throws IOException {
        if (variant.isOk())
            throw new IllegalStateException("Result#printToStream should only be called on error values");
        out.write(message);
    }

    public enum Variant implements Result.Variant {
        Valid(true), Invalid(false);

        public final boolean isOk;

        Variant(boolean isOk) {
            this.isOk = isOk;
        }

        @Override
        public boolean isOk() {
            return isOk;
        }
    }
}
