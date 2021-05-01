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
import java.nio.file.Path;
import java.nio.file.Paths;

// TODO: Warn if non-test files are found in tests/
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
        path = Glob.readSymbolicLink(path);
        String mime = Glob.mime(path);
        if (!mime.equals("application/xml") && !mime.equals("text/xml"))
            throw new UserErrorException(String.format("Test must be of type xml, but `%s' is of type %s", path, mime));
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

    public TestValidationResult validate(File file) throws IOException {
        try {
            load(file);
            return new TestValidationResult(file, TestValidationResult.Variant.VALID);
        }
        catch (UserErrorException e) {
            return new TestValidationResult(file, TestValidationResult.Variant.INVALID, (SAXException) e.getCause());
        }
    }

    public static class Factory {
        private final Schema schema;

        public Factory() throws SAXException {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
            factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
            schema = factory.newSchema(
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
