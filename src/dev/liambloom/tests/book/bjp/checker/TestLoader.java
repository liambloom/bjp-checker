package dev.liambloom.tests.book.bjp.checker;

import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestLoader {
    public static final String DEFAULT_TEST = "bjp3";
    private final Validator validator;

    TestLoader(Validator validator) {
        this.validator = validator;
    }

    public Source load() throws URISyntaxException, IOException {
        return load(DEFAULT_TEST);
    }

    public Source load(String name) throws URISyntaxException, IOException {
        return load(Paths.get(App.here + File.separator + "tests" + File.separator + name + ".xml"));
    }

    public Source load(File file) throws IOException {
        return load(file.toPath());
    }

    public Source load(Path path) throws IOException {
        while (Files.isSymbolicLink(path))
            path = Files.readSymbolicLink(path);
        if (Files.isDirectory(path))
            throw new UserErrorException("Test must be of type xml, found directory instead");
        String mime = Files.probeContentType(path);
        if (mime.equals("application/xml") || mime.equals("text/xml"))
            throw new UserErrorException("Test must be of type xml, found " + mime + "instead");
        Source source = new StreamSource(path.toFile());
        try {
            validator.reset();
            validator.validate(source);
            return source;
        }
        catch (SAXException e) {
            throw new UserErrorException(e);
        }
    }

    public Result<TestValidationResult> validate(File file) throws IOException {
        try {
            load(file);
            // TODO: Result doesn't make sense
            return new Result<>(getTestName(file), TestValidationResult.Valid);
        }
        catch (UserErrorException e) {
            assert e.getCause() instanceof SAXException;
            return new Result<>(getTestName(file), TestValidationResult.Invalid, e);
        }
    }

    public static class Factory {
        private final Schema schema;

        public Factory() throws SAXException {
            schema = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(
                    new StreamSource(getClass().getResourceAsStream("/book-tests.xsd")));
        }

        public TestLoader newTestLoader() {
            return new TestLoader(schema.newValidator());
        }
    }

    public static String getTestName(File file) {
        String name = file.getName();
        return name.substring(0, name.length() - 4);
    }
}
