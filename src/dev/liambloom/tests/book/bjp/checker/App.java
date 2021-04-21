package dev.liambloom.tests.book.bjp.checker;

import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class App {
    public static final String VERSION = "v1.0.0-alpha-1";

    /**
     * The location of this application's files. The parent folder of "lib" and "bin"
     */
    public final String here;
    public Logger logger;

    public App(Logger logger) throws URISyntaxException {
        this.logger = logger;
        File f = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        if (f.isFile())
            f = f.getParentFile();
        here = f.getParent();
    }

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
            return load(Paths.get(here + File.separator + "tests" + File.separator + name + ".xml"));
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

        private <T> Result validate(LoaderFunctionalInterface<T> loader, T src) throws IOException {
            try {
                loader.load(src);
                // TODO: Result doesn't make sense
                return Result.Correct;
            }
            catch (UserErrorException e) {
                assert e.getCause() instanceof SAXException;
                Result res = Result.Incorrect;
                res.error = e.getCause();
                return res;
            }
        }

        public Result validate(File path) throws IOException {
            return validate(this::load, path);
        }

        public Result validate(Path path) throws IOException {
            return validate(this::load, path);
        }
    }

    private class TestLoaderFactory {
        private final Schema schema;

        public TestLoaderFactory() throws SAXException {
            schema = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1").newSchema(
                    new StreamSource(getClass().getResourceAsStream("/book-tests.xsd")));
        }

        public TestLoader newTestLoader() {
            return new TestLoader(schema.newValidator());
        }
    }

    @FunctionalInterface
    private interface LoaderFunctionalInterface<T> {
        Source load(T t) throws IOException;
    }

    public void createLogFile(Throwable err) throws IOException {
        final File log = new File(here
                + File.separator + "logs" + File.separator
                + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
        log.getParentFile().mkdir();
        log.createNewFile();
        err.printStackTrace(new PrintStream(log));
    }

    public Results validateTests() throws SAXException, IOException, InterruptedException {
        final int threadCount = Runtime.getRuntime().availableProcessors();
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final TestLoaderFactory loaderFactory = new TestLoaderFactory();

        File[] tests = new File(here + File.separator + "tests" ).listFiles((file, name) -> {
            if (file.isDirectory() || file.getName().endsWith(".xml")) {
                logger.warn("Expected xml file, found %s `%s' in tests", file.isDirectory() ? "directory" : "file", file.getName());
                return false;
            }
            return true;
        });

        if (tests == null)
            throw new UserErrorException(new FileNotFoundException("Folder `tests' not found at " + here + File.separator + "tests"));
        if (tests.length == 0) {
            logger.warn("No tests found in " + here + File.separator + "tests");
            return new Results();
        }

        // TODO: Don't create a new testLoader every iteration
        Results results = new Results(Arrays.stream(tests).map(file -> executor.submit(() -> loaderFactory.newTestLoader().validate(file))));
        executor.shutdown();
        if (!executor.awaitTermination(30L, TimeUnit.SECONDS))
            throw new UserErrorException("Took more than 30 seconds to validate tests");
        return results;
    }
}
