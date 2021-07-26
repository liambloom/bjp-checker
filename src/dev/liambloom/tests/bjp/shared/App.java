package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.Chapter;
import dev.liambloom.tests.bjp.Exercise;
import dev.liambloom.tests.bjp.ProgrammingProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    private App() {}

    public static final String VERSION = "v1.0.0-alpha-2";
    private static final String[] TEST_NAMES = { "bjp3" };

    /**
     * The location of this application's files. The parent folder of "lib" and "bin"
     */
    private static Path here = null;
    private static Preferences prefs = null;
    private static Logger innerLogger = null;
    // TODO: Use logger more & better
    public final static Logger logger = (logKind, msg, args) -> innerLogger.log(logKind, msg, args);

    public static Path here() {
        if (here == null) {
            try {
                Path p = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (Files.isRegularFile(p))
                    p = p.getParent();
                here = p.getParent();
            }
            catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return here;
    }

    public static Preferences prefs() {
        if (prefs == null)
            prefs = Preferences.userRoot().node("dev/liambloom/tests/bjp");
        return prefs;
    }

    /*public static Stream<InputStream> testBase() {
        Stream.Builder<InputStream> builder = Stream.builder();

        builder.add();



        if (testBase == null)
            testBase = here().resolve("tests");
        return testBase;
    }*/

    /* TODO: Create a custom class for managing these tests, which loads them, stores their names,
        InputStreams, etc. The test should be called Book. */
    public static InputStream getTest(String name) {
        return Stream.<Supplier<Optional<InputStream>>>of(
                () -> Optional.ofNullable(App.class.getClassLoader().getResourceAsStream("/tests/" + name + ".xml")),
                () -> Optional.ofNullable(App.prefs().node("tests").get(name, null)).map(s -> {
                    try {
                        return new BufferedInputStream(new FileInputStream(s));
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                })
        )
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .map(Optional::get)
                .findFirst() // Streams being lazy is so wonderful
                .orElseThrow(() -> new UserErrorException("Test \"" + name + "\" not found"));
    }

    /**
     * This gets a stream of paths to each custom test. Tests that come built
     * into the application are not in this stream. Not all paths are
     * guaranteed to exist, and the caller of this method should handle
     * any non-existent paths, generally by altering the user.
     *
     * @return A stream of paths to each custom test
     */
    public static Stream<Path> getCustomTests() {
        Preferences tests = prefs().node("tests");
        Preferences index = tests.node("index");
        Stream.Builder<Path> builder = Stream.builder();

        for (int i = 0; i < index.getInt("size", 0); i++) {
            String testName = index.get(Integer.toString(i), null);
            if (testName == null)
                continue;
            String test = tests.get(testName, null);
            if (test == null)
                continue;
            builder.add(Path.of(test));
        }

        return builder.build();
    }

    public static Stream<InputStream> getAllTests() {
        return Stream.concat(
                Arrays.stream(TEST_NAMES)
                        .map(n -> App.class.getClassLoader().getResourceAsStream("/tests" + n + ".xml")),
                getCustomTests()
                        .map(p -> {
                            try {
                                return new BufferedInputStream(new FileInputStream(p.toFile()));
                            } catch (FileNotFoundException e) {
                                // TODO
                            }
                        })
        );
    }


    public static void setLogger(Logger logger) {
        App.innerLogger = logger;
    }

    public static Schema loadTestSchema() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
        // TODO: factory.setErrorHandler(ErrorHandler)
        return factory.newSchema(
                new StreamSource(App.class.getResourceAsStream("/book-tests.xsd")));
    }

    // TODO: Use this somewhere
    /*public static void checkTests() throws IOException {
        for (Path p : Files.exists(App.testBase()) ? Files.newDirectoryStream(App.testBase()) : Collections.<Path>emptyList()) {
            p = p.toRealPath();
            if (Files.isDirectory(p) || !p.toString().endsWith(".xml"))
                logger.log(Logger.LogKind.WARN, "Expected xml file, found `%s' in tests", p.toString());
        }
    }*/

    public static void createLogFile(Throwable err) throws IOException {
        final Path log = here().resolve("logs").resolve(DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
        Files.createDirectory(log.getParent());
        Files.createFile(log);
        err.printStackTrace(new PrintStream(Files.newOutputStream(log)));
        // System.err.println(log); // I can't remember if this was just for debugging
    }

    public static Stream<Result> validateTests(Stream<InputStream> paths) throws SAXException, IOException {
        try {
            //final TestLoader.Factory loaderFactory = new TestLoader.Factory();
            final Schema schema = loadTestSchema();
            final Queue<Validator> queue = new ConcurrentLinkedQueue<>();

            return paths
                    .map((FunctionThrowsIOException<Path, Result>) (path -> {
                        path = path.toRealPath();
                        if (!path.toString().endsWith(".xml"))
                            throw new UserErrorException(String.format("Test must be of type xml, but `%s' is not", path));
                        Source source = new StreamSource(path.toFile());
                        // TODO: Use error logger that is build into this
                        Validator v = queue.poll();
                        if (v == null)
                            v = schema.newValidator();
                        else
                            v.reset();
                        try {
                            v.validate(source);
                            return new Result(
                                    path.toString().substring(0, path.toString().lastIndexOf('.')),
                                    TestValidationStatus.VALID/* ,
                                    TODO: Generate ByteArrayOutputStream from ErrorHandler*/);
                        }
                        catch (SAXException e) {
                            // TODO: Wait, what? What is this? This is not what results are for!
                            throw new UserErrorException(e);
                        }
                        finally {
                            queue.add(v);
                        }
                    }));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public static Stream<Result> check(CheckArgs args)
            throws IOException, NoSuchMethodException, SAXException, ParserConfigurationException, XPathExpressionException {
        List<Class<?>> classes;
        int chapter;
        try {
            AtomicInteger detectedChapter = new AtomicInteger(-1);
            classes = new PathClassLoader(args.paths()).loadAllClasses()
                    .filter(clazz -> {
                    Chapter ch = clazz.getAnnotation(Chapter.class);
                        if (ch == null)
                            return false;
                        else if (ch.value() <= 0 || ch.value() > CheckArgs.MAX_CH)
                            throw new UserErrorException("There is no chapter " + ch.value());
                        else if (args.chapter().isPresent()) {
                            return ch.value() == args.chapter().getAsInt();
                        }
                        else if (!detectedChapter.compareAndSet(-1, ch.value()))
                            throw new UserErrorException("Cannot auto detect chapter, as classes belonging to chapters " + ch.value() + " and " + detectedChapter + " were found");
                        return true;
                    })
                    .collect(Collectors.toList());
            chapter = args.chapter().orElseGet(detectedChapter::get);
        }
        catch (LinkageError e) {
            throw new UserErrorException(e);
        }

        Document document = args.tests();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath1 = xpf.newXPath();
        Node ch = (Node) xpath1.evaluate("/book/chapter[@num='" + chapter  + "']", document, XPathConstants.NODE);
        Queue<XPath> xpaths = new ConcurrentLinkedQueue<>();
        xpaths.add(xpath1);


        return Stream.of(new CheckerTargetGroup(Exercise.class, args.exercises()), new CheckerTargetGroup(ProgrammingProject.class, args.programmingProjects()))
                .map(e -> e.addPotentialTargets(classes.iterator()))
                .flatMap(CheckerTargetGroup::tests)
                .map(d -> {
                    XPath xpath = Optional.ofNullable(xpaths.poll()).orElseGet(xpf::newXPath);
                    try {
                        return d.newTest(ch, xpath);
                    }
                    finally {
                        xpaths.add(xpath);
                    }
                })
                .map(Test::run);
    }

    private static class CheckerTargetGroup {
        private final List<AnnotatedElement>[] targets;
        private final Class<? extends Annotation> annotationType;
        private final Function<Annotation, Integer> valueOf;

        @SuppressWarnings("unchecked")
        public CheckerTargetGroup(Class<? extends Annotation> annotationType, boolean[] initWhich) throws NoSuchMethodException {
            targets = new ArrayList[initWhich.length];
            for (int i = 0; i < CheckArgs.MAX_EX_COUNT; i++) {
                if (initWhich[i])
                    targets[i] = Collections.synchronizedList(new ArrayList<>());
            }

            Method m = annotationType.getMethod("value");
            valueOf = a -> {
                try {
                    return (Integer) m.invoke(a);
                }
                catch (IllegalAccessException | InvocationTargetException e) { throw new IllegalStateException(e); }
            };

            this.annotationType = annotationType;
        }

        public void addPotentialTarget(AnnotatedElement e) {
            Optional.ofNullable(targets[valueOf.apply(e.getAnnotation(annotationType))])
                    .ifPresent(t -> t.add(e));
        }

        public CheckerTargetGroup addPotentialTargets(Iterator<? extends AnnotatedElement> iter) {
            iter.forEachRemaining(this::addPotentialTarget);
            return this;
        }

        public Stream<Test.Target> tests() {
            Stream.Builder<Test.Target> builder = Stream.builder();
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null){
                    /*if (targets[i].isEmpty())
                        throw new UserErrorException("Unable to find " + TestUtils.camelOrPascalToSpacedCase(annotationType.getSimpleName()) + i);*/
                    builder.add(new Test.Target(targets[i], annotationType, i));
                }
            }
            return builder.build();
        }
    }
}
