package dev.liambloom.tests.book.bjp.checker;

import dev.liambloom.tests.book.bjp.Chapter;
import dev.liambloom.tests.book.bjp.Exercise;
import dev.liambloom.tests.book.bjp.ProgrammingProject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    public static final String VERSION = "v1.0.0-alpha-2";

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

    static Schema loadTestSchema() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
        return factory.newSchema(
                new StreamSource(App.class.getResourceAsStream("/book-tests.xsd")));
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
        final File log = new File(here
                + File.separator + "logs" + File.separator
                + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
        log.getParentFile().mkdir();
        log.createNewFile();
        err.printStackTrace(new PrintStream(log));
        System.err.println(log);
    }

    public Stream<TestValidationResult> validateTests(String[] glob) throws SAXException, IOException {
        if (glob.length == 0)
            glob = new String[]{ "@tests" };
        return validateTests(new Glob(glob, true, logger));
    }

    public Stream<TestValidationResult> validateTests(Glob glob) throws SAXException, IOException {
        try {
            //final TestLoader.Factory loaderFactory = new TestLoader.Factory();
            final Schema schema = loadTestSchema();
            final Queue<Validator> queue = new ConcurrentLinkedQueue<>();

            return glob.files()
                    .map((FunctionThrowsIOException<File, TestValidationResult>) (file -> {
                        Path path = Glob.readSymbolicLink(file.toPath());
                        if (!path.toString().endsWith(".xml"))
                            throw new UserErrorException(String.format("Test must be of type xml, but `%s' is not", path));
                        Source source = new StreamSource(path.toFile());
                        Validator v = queue.poll();
                        if (v == null)
                            v = schema.newValidator();
                        else
                            v.reset();
                        try {
                            v.validate(source);
                            return new TestValidationResult(file, TestValidationResult.Variant.VALID);
                        }
                        catch (SAXException e) {
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

    public Stream<TestResult> check(CheckArgs args) throws IOException, NoSuchMethodException, SAXException, ParserConfigurationException {
        List<Class<?>> classes;
        int chapter;
        try {
            AtomicInteger detectedChapter = new AtomicInteger(-1);
            classes = new SecureGlobClassLoader(args.glob()).loadAllClasses()
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




        Stream.of(new CheckerTargetGroup(Exercise.class, args.exercises()), new CheckerTargetGroup(ProgrammingProject.class, args.programmingProjects()))
                .map(e -> e.addPotentialTargets(classes.iterator()))
                .flatMap(CheckerTargetGroup::tests)
                ;

        return null; // TODO
    }

    class CheckerTargetGroup {
        private final Pattern LOOKAHEAD_CAPITAL = Pattern.compile("(?=[A-Z])");
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
            if (m.getReturnType() != int.class)
                throw new IllegalArgumentException("Method " + m + " should return int");
            if (!m.isAccessible())
                throw new IllegalStateException("Impossible state: inaccessible method in interface");
            valueOf = a -> {
                try {
                    return (Integer) m.invoke(a);
                }
                catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Impossible state: this should have been caught earlier", e);
                }
            };

            this.annotationType = annotationType;
        }

        public CheckerTargetGroup addPotentialTarget(AnnotatedElement e) {
            for (Annotation a : e.getAnnotationsByType(annotationType)) {
                Optional.ofNullable(targets[valueOf.apply(a)])
                        .ifPresent(t -> t.add(e));
            }
            return this;
        }

        public CheckerTargetGroup addPotentialTargets(Iterator<? extends AnnotatedElement> iter) {
            iter.forEachRemaining(this::addPotentialTarget);
            return this;
        }

        public Stream<Test.Target> tests() {
            Stream.Builder<Test.Target> builder = Stream.builder();
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null){
                    if (targets[i].isEmpty())
                        throw new UserErrorException("Unable to find " + LOOKAHEAD_CAPITAL.matcher(annotationType.getSimpleName()).replaceAll(" ").toLowerCase() + i);
                    builder.add(new Test.Target(targets[i], annotationType, i));
                }
            }
            return builder.build();
        }
    }

    // The JVM has a glob parser that runs only on windows. It is not a very good
    // glob parser. In order to prevent the JVM from running it, I append (char) 3
    // to the end of each argument in rust. Since this is not allowed in paths,
    // it prevents the JVM from trying to parse globs.
    public static void cleanArgs(String[] args) {
        for (int i = 0; i < args.length; i++)
            if (args[i].charAt(args[i].length() - 1) == '\t')
                args[i] = args[i].substring(0, args[i].length() - 1);
    }
}
