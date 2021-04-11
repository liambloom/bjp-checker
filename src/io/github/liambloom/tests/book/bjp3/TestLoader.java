package io.github.liambloom.tests.book.bjp3;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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

class TestLoader {
    // TODO: This should be a ReadWriteLock
    private static Schema schema;

    public static Schema getSchema() throws SAXException {
        if (schema == null)
            schema = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(
                    new StreamSource(TestLoader.class.getResourceAsStream("/book-tests.xsd")));
        return schema;
    }

    // TODO: split up into several methods and lazy fields
    public static /*Map<String, Source>*/ Source[] load() throws SAXException, URISyntaxException, IOException {
        // TODO: Fix conflicting SAXExceptions
        // If a SAXException is thrown here, it is my fault
        Schema schema = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(new StreamSource(TestLoader.class.getResourceAsStream("/book-tests.xsd")));

        Source[] tests;
        final File externalTests = new File(new File(App.app.here).getParent() + File.separator + "tests");
        if (!externalTests.exists())
            tests = new Source[1];
        else {
            File[] sources = externalTests.listFiles();
            tests = new Source[sources.length + 1];
            for (int i = 0; i < sources.length; i++) {
                Path p = sources[i].toPath();
                if (Files.isSymbolicLink(p))
                    p = Files.readSymbolicLink(p);
                if (sources[i].isDirectory()) {
                    App.debugger.warn("Expected an xml file at " + p + ", but found directory instead");
                }
                final String mime = Files.probeContentType(p);
                if (mime.equals("application/xml") || mime.equals("text/xml"))
                    App.debugger.warn("Expected an xml file at " + p + ", but found " + mime + " instead");
                //if (Files.isSymbolicLink(sources[i].toPath()))

            }
        }
        tests[0] = new StreamSource(TestLoader.class.getResourceAsStream("/tests.xml"));

        // System.out.println(Files.probeContentType(Paths.get(TestLoader.class.getResource("/tests.xml").toURI())));
        //Files.readSymbolicLink(Paths.get(TestLoader.class.getResource("/tests.xml").toURI()));

        // TODO: Only validate the built in tests (the one in the jar) in debug mode

        Validator validator = schema.newValidator();
        for (int i = 0; i < tests.length; i++) {
            if (i == 0 && !App.debugger.debugMode)
                continue;
            // If a SAXException is thrown here, it is the user's fault\
            try {
                validator.validate(tests[i]);
            }
            catch (SAXParseException e) {
                throw new UserErrorException(e);
            }
            validator.reset();
        }

        return tests;
    }
}
