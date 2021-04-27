package dev.liambloom.tests.book.bjp.checker;

import org.xml.sax.SAXException;

import java.io.*;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class App {
    public static final String VERSION = "v1.0.0-alpha";

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
            App.createLogFile(e);
            throw new RuntimeException("Checker install location could not be converted to URI", e);
        }
    }

    public Logger logger;

    public App(Logger logger) throws IOException {
        this.logger = logger;
        for (File f : Glob.TEST_BASE.listFiles()) {
            f = Glob.readSymbolicLink(f);
            String mime = f.isDirectory() ? "directory" : Glob.mime(f.toPath());
            if (!mime.equals("text/xml") && !mime.equals("application/xml"))
                logger.warn("Expected xml file, found `%s' of type %s in tests", f.getName(), mime);
        }
    }

    public static void createLogFile(Throwable err) {
        try {
            final File log = new File(here
                    + File.separator + "logs" + File.separator
                    + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
            log.getParentFile().mkdir();
            log.createNewFile();
            err.printStackTrace(new PrintStream(log));
        }
        catch (IOException e) {
            // ¯\_(ツ)_/¯
        }
    }

    public Stream<TestValidationResult> validateTests(String[] glob) throws SAXException, IOException {
        if (glob.length == 0)
            glob = new String[]{ "@tests" };
        try {
            final TestLoader.Factory loaderFactory = new TestLoader.Factory();
            final Queue<TestLoader> queue = new ConcurrentLinkedQueue<>();

            return new Glob(glob, true, logger).files()
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
}
