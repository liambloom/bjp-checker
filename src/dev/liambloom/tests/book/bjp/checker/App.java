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
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class App {
    public static final String VERSION = "v1.0.0-alpha-1";

    /**
     * The location of this application's files. The parent folder of "lib" and "bin"
     */
    public static final String here;

    static {
        try {
            File f = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (f.isFile())
                f = f.getParentFile();
            here = f.getParent();
        }
        catch (URISyntaxException e) {
            try {
                App.createLogFile(e);
            } catch (IOException ignored) { }
            throw new RuntimeException("Checker install location could not be converted to URI");
        }
    }

    public Logger logger;

    public App(Logger logger) throws URISyntaxException {
        this.logger = logger;
    }

    public static void createLogFile(Throwable err) throws IOException {
        final File log = new File(here
                + File.separator + "logs" + File.separator
                + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
        log.getParentFile().mkdir();
        log.createNewFile();
        err.printStackTrace(new PrintStream(log));
    }

    public Stream<Result<TestValidationResult>> validateTests(Glob glob) throws SAXException, IOException {
        final TestLoader.Factory loaderFactory = new TestLoader.Factory();
        final Queue<TestLoader> queue = new ConcurrentLinkedQueue<>();
        AtomicReference<IOException> exception = new AtomicReference<>();

        Stream<Result<TestValidationResult>> r = glob.files()
                .filter(file -> {
                    if (file.isDirectory() || file.getName().endsWith(".xml")) {
                        logger.warn("Expected xml file, found %s `%s' in tests", file.isDirectory() ? "directory" : "file", file.getName());
                        return false;
                    }
                    return true;
                })
                .map(file -> {
                    TestLoader loader = Optional.ofNullable(queue.poll()).orElseGet(loaderFactory::newTestLoader);
                    try {
                        return loader.validate(file);
                    }
                    catch (IOException e) {
                        try {
                            App.createLogFile(e);
                        } catch (IOException ignored) {};
                        exception.compareAndSet(null, e);
                        return null;
                    }
                    finally {
                        queue.add(loader);
                    }
                });
        if (exception.get() != null)
            throw exception.get();
        assert r.isParallel();
        return r;
    }
}
