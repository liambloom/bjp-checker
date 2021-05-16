package dev.liambloom.tests.book.bjp.checker;

import org.xml.sax.SAXException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    public static final String VERSION = "v1.0.0-alpha";

    /**
     * The location of this application's files. The parent folder of "lib" and "bin"
     */
    private static String here = null;

    public static String here() {
        if (here == null) {
            try {
                File f = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (f.isFile())
                    f = f.getParentFile();
                here = f.getParent();
            }
            catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return here;
    }

    private static File testBase = null;

    public static File testBase() {
        if (testBase == null)
            testBase = new File(here(), "tests");
        return testBase;
    }

    public Logger logger;

    public App(Logger logger) throws IOException {
        this.logger = logger;
        for (File f : Optional.ofNullable(App.testBase().listFiles()).orElseGet(() -> new File[0])) {
            f = Glob.readSymbolicLink(f);
            if (f.isDirectory() || !f.toString().endsWith(".xml"))
                logger.warn("Expected xml file, found `%s' in tests", f.getName());
        }
    }

    public static void createLogFile(Throwable err) throws IOException {
        //try {
            final File log = new File(here
                    + File.separator + "logs" + File.separator
                    + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
            log.getParentFile().mkdir();
            log.createNewFile();
            err.printStackTrace(new PrintStream(log));
            System.err.println(log);
//        }
//        catch (IOException e) {
//            //throw new UncheckedIOException(e);
//            // System.err.println("Failed to create logfile");
//            // ¯\_(ツ)_/¯
//        }
    }

    public Stream<TestValidationResult> validateTests(String[] glob) throws SAXException, IOException {
        if (glob.length == 0)
            glob = new String[]{ "@tests" };
        return validateTests(new Glob(glob, true, logger));
    }

    public Stream<TestValidationResult> validateTests(Glob glob) throws SAXException, IOException {
        try {
            final TestLoader.Factory loaderFactory = new TestLoader.Factory();
            final Queue<TestLoader> queue = new ConcurrentLinkedQueue<>();

            return glob.files()
                    .map((FunctionThrowsIOException<File, TestValidationResult>) (file -> {
                        TestLoader loader = Optional.ofNullable(queue.poll()).orElseGet(loaderFactory::newTestLoader);
                        try {
                            return loader.validate(file);
                        }
                        finally {
                            queue.add(loader);
                        }
                    }));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public Stream<TestResult> check(Glob glob) throws IOException {
        for (Class<?> c : new SecureGlobClassLoader(glob).loadAll()) {
            System.out.println(c);
        }
        return null; // TODO
    }
}
