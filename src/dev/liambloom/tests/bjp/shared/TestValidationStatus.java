package dev.liambloom.tests.bjp.shared;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.regex.Pattern;

/*public class TestValidationResult extends Result {
    public final byte[] message;

    public TestValidationResult(Path path, Status variant) throws IOException {
        this(path, variant, null);
    }

    public TestValidationResult(Path path, Status variant, SAXException error) throws IOException {
        super(path.toString().substring(0, path.toString().lastIndexOf('.')), variant);

        if (error == null) {
            message = null;
            return;
        }

        String message = error.getMessage();
        if (message.endsWith("."))
            message = message.substring(0, message.length() - 1);

        if (error instanceof SAXParseException parseException)
            message += String.format(" at %s:%d:%d", path.toRealPath(), parseException.getLineNumber(), parseException.getColumnNumber());

        this.message = message.getBytes();
    }

    @Override
    public void printToStream(OutputStream out) throws IOException {
        if (status.isOk())
            throw new IllegalStateException("Result#printToStream should only be called on error values");
        out.write(message);
    }*/

    public enum TestValidationStatus implements Result.Status {
        VALID(Color.GREEN),
        NOT_FOUND(Color.YELLOW),
        INVALID(Color.RED);

        private final Color color;

        TestValidationStatus(Color color) {
            this.color = color;
        }

        @Override
        public Color color() {
            return color;
        }
    }
//}
