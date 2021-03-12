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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class TestLoader {

    public static Source[] load() {
        try {
            // TODO: Fix conflicting SAXExceptions
            // If a SAXException is thrown here, it is my fault
            Schema schema = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(new StreamSource(TestLoader.class.getResourceAsStream("/book-tests.xsd")));

            Source[] tests;
            final URL externalTests = TestLoader.class.getResource("/../tests");
            if (externalTests == null)
                tests = new Source[1];
            else {
                File[] sources = new File(externalTests.toURI()).listFiles();
                tests = new Source[sources.length + 1];
                for (int i = 0; i < sources.length; i++) {
                    Path p = sources[i].toPath();
                    if (Files.isSymbolicLink(p))
                        p = Files.readSymbolicLink(p);
                    System.out.println(Files.probeContentType(p));
                    //if (Files.isSymbolicLink(sources[i].toPath()))

                }
            }
            tests[0] = new StreamSource(TestLoader.class.getResourceAsStream("/tests.xml"));

            Validator validator = schema.newValidator();
            for (Source test : tests) {
                // If a SAXException is thrown here, it is the user's fault
                validator.validate(test);
                validator.reset();
            }

            return tests;
        }
        catch (SAXException | URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
