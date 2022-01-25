package dev.liambloom.checker;

import dev.liambloom.checker.books.BookLocator;
import dev.liambloom.checker.books.Result;
import dev.liambloom.util.function.ConsumerThrowsException;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Note: this is NOT thread safe.
 */
public final class BookChecker {
    private static final Path initDir = Path.of(".").toAbsolutePath().normalize();
    private static final Thread dirResetter = new Thread(BookChecker::resetDir);
    private static final DocumentBuilderFactory dbf;
    private static final Lock nativeLoaderLock = new ReentrantLock();
    private static boolean triedLoadNative;
    private static boolean loadedNativeSuccess;

    // Set at initialization
    private final System.Logger logger = System.getLogger(Long.toString(System.identityHashCode(this)));
    private final BookLocator book;
    private final String name;
    private MessageDigest digest;

    // Set in parseAndValidateDocument()
    private Document document = null;
    private ClassLoader classLoader;
    private Result<TestValidationStatus> result = null;
    private Exception documentParseException;
    private CheckableType<? extends Annotation> chapterType = null;
    private CheckableType<? extends Annotation>[] checkableTypes = null;

    // Set in check()
    private Path tempDir = null;

    // Set in validate()
    private boolean isInvalidated = false;

    // Set in getCheckableTypeSet()
    private Set<String> checkableTypeSet = null;

    // Set in getResources()
    private String[] resources = null;

    public BookChecker(BookLocator book) {
        this(book.toString(), book);
    }

    public BookChecker(String name, BookLocator book) {
        this.name = name;
        this.book = book;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                removeTempDir();
            }
            catch (IOException e) {
                System.getLogger(BookChecker.class.getName()).log(System.Logger.Level.ERROR, "Error removing temp dir", e);
            }
        }));
    }

    private Stream<Element> elementOfType(String... types) {
        return Arrays.stream(types)
            .map(document::getElementsByTagName)
            .flatMap(Util::streamNodeList)
            .filter(Element.class::isInstance)
            .map(Element.class::cast);
    }

    public Result<TestValidationStatus> validateBook() throws IOException {
        try {
            parseAndValidateDocument();
        }
        catch (IOException e) {
            throw e;
        }
        catch (Error | RuntimeException e) {
            if (e != documentParseException)
                throw e;
        }
        catch (Exception e) {
            if (e != documentParseException)
                throw new IllegalStateException("Checked exception thrown during document parsing, but result is null", e);
        }
        assert result != null;
        return result;
    }

    private Document getDocument() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        parseAndValidateDocument();
        return document;
    }

    private CheckableType<? extends Annotation> getChapterType() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        parseAndValidateDocument();
        return chapterType;
    }

    private CheckableType<? extends Annotation>[] getCheckableTypes() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        parseAndValidateDocument();
        return checkableTypes;
    }

    public Set<String> getCheckableTypeSet() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        return checkableTypeSet == null ? checkableTypeSet = Arrays.stream(getCheckableTypes()).map(CheckableType::name).collect(Collectors.toCollection(HashSet::new)) : checkableTypeSet;
    }

    private String[] getResources() throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException {
        if (resources != null)
            return resources;

        NodeList nodes;
        try {
            nodes = (NodeList) xpath.evaluate("/book/meta/rsc", getDocument(), XPathConstants.NODESET);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        resources = new String[nodes.getLength()];
        for (int i = 0; i < resources.length; i++)
            resources[i] = ((Element) nodes.item(i)).getAttribute("href");
        return resources;
    }

    /**
     * Checks according to args
     * @param chapter             The chapter to check, or {@code OptionalInt.empty()} to auto-detect.
     * @param checkables          A map between annotations for checkables and the checkables of that type to run.
     * @param targets             A stream of classes that should be checked
     * @return A stream containing the results of all checks
     *
     * @throws IOException If an i/o error occurs
     * @throws ClassNotFoundException If the book references a class that could not be found
     * @throws SAXException If a SAXException is thrown when parsing the book
     * @throws ClassCastException If the book's chapter or checkable annotations are not actually annotations, or if their
     *              {@code value()} methods don't return {@code int}s.
     * @throws NoSuchMethodException If the book's chapter or checkable annotations don't have a {@code value()} method
     * @throws IllegalAccessException If the book's chapter or checkable annotations' {@code value()} method is not accessible
     * @throws InvocationTargetException If the book's chapter or checkable annotations' {@code value()} method throws an exception
     */
    @SuppressWarnings("RedundantThrows")
    public Result<TestStatus>[] check(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalInt chapter, Map<String, boolean[]> checkables,
                                      Stream<Path> targets) throws IOException, ClassNotFoundException, SAXException, NoSuchMethodException,
        IllegalAccessException, InvocationTargetException {
        getDocument();

        NodeList metadataNodes;
//        chapterAnnotation;
//        Map<String, Class<? extends Annotation>> checkableAnnotations = new HashMap<>();
//        checkableAnnotations;
//        Stream<String> resources;
        {
            try {
                metadataNodes = ((Element) xpath.evaluate("/book/meta", document, XPathConstants.NODE)).getChildNodes();
            }
            catch (XPathExpressionException e) {
                throw new RuntimeException(e);
            }
            finally {
                Util.getXPathPool().offer(xpath);
            }
        }

        Element[] meta = Util.streamNodeList(metadataNodes)
            .filter(Element.class::isInstance)
            .map(Element.class::cast)
            .toArray(Element[]::new);
        Class<? extends Annotation> chapterAnnotation = getChapterType().annotation();

        for (String checkable : checkables.keySet()) {
            if (!getCheckableTypeSet().contains(checkable))
                throw new IllegalArgumentException("Checkable \"" + checkable + "\" does not exist");
        }

        Method m = chapterAnnotation.getMethod("value");

        int checkedChapter;
        logger.log(System.Logger.Level.DEBUG, "Chapter annotation type: %s", chapterAnnotation);
//        logger.log(System.Logger.Level.DEBUG, "Target annotation types: %s", );s
        AtomicInteger detectedChapter = new AtomicInteger(-1);
        List<Class<?>> classes = new PathClassLoader(targets, classLoader).loadAllOwnClasses()
//            .peek(System.out::println)
            .filter(clazz -> {
                System.Logger logger = System.getLogger(Util.generateLoggerName());
                logger.log(System.Logger.Level.TRACE, "%s has annotations %s", clazz, Arrays.toString(clazz.getAnnotations()));
                return Arrays.stream(clazz.getAnnotationsByType(chapterAnnotation))
                    .map(FunctionUtils.unchecked((FunctionThrowsException<Annotation, Integer>) a -> {
                        m.trySetAccessible();
                        return (int) m.invoke(a);
                    }))
                    .anyMatch(c -> {
                        logger.log(System.Logger.Level.TRACE, "Class %s belongs to chapter %d, expecting %s", clazz, c, chapter);
                        if (chapter.isPresent())
                            return c == chapter.getAsInt();
                        else if (detectedChapter.compareAndExchange(-1, c) != c)
                            throw new IllegalArgumentException("Cannot auto detect chapter, as classes belonging to chapters "
                                + c + " and " + detectedChapter + " were found");
                        else
                            return true;
                    });
                }
            )
            .collect(Collectors.toList());
        logger.log(System.Logger.Level.TRACE, "Classes: " + classes);
        checkedChapter = chapter.orElseGet(detectedChapter::get);

        Targets potentialTargets = new Targets();
        Queue<Class<?>> classQ = new LinkedList<>(classes);
        while (!classQ.isEmpty()) {
            Class<?> c = classQ.remove();
            potentialTargets.classes().add(c);
            Set<Class<?>> inner = new HashSet<>();
            Collections.addAll(inner, c.getClasses());
            Collections.addAll(inner, c.getDeclaredClasses());
            classQ.addAll(inner);
        }
        potentialTargets.classes()
            .stream()
            .flatMap(c -> Stream.of(c.getFields(), c.getDeclaredFields(), c.getMethods(), c.getDeclaredMethods(), c.getDeclaredConstructors()))
            .flatMap(Stream::of)
            .forEach(potentialTargets::add);

        Runtime.getRuntime().addShutdownHook(dirResetter);
        if (tempDir == null)
            tempDir = Files.createTempDirectory(null);
        changeDirectory(book.loadResources(tempDir, getResources()));

        Node ch;
        logger.log(System.Logger.Level.DEBUG, "Chapter %d", checkedChapter);
        try {
            ch = (Node) xpath.evaluate("/book/chapter[@num='" + checkedChapter + "']", document, XPathConstants.NODE);
            logger.log(System.Logger.Level.TRACE, "Chapter %d is %s", checkedChapter, ch);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }

        if (ch == null)
            throw new NoSuchElementException((chapter.isPresent() ? "S" : "Auto-detected s") + "ection " + checkedChapter + " does not exist");

        InputStream prevIn = System.in;
        PrintStream prevOut = System.out;
        PrintStream prevErr = System.err;
        try {
             List<Future<Result<TestStatus>>> futures = Arrays.stream(getCheckableTypes())
                 .parallel()
                 .map(a -> new CheckerTargetGroup<>(a, checkables.getOrDefault(a.name(), new boolean[0])))
                 .flatMap(FunctionUtils.unchecked((FunctionThrowsException<CheckerTargetGroup<?>, Stream<Test>>) (e -> {
                     for (AnnotatedElement potential : potentialTargets)
                        e.addPotentialTarget(potential);
                     return e.apply(ch);
                 })))
                 .map(Test::start)
                 .collect(Collectors.toList());
             Result<TestStatus>[] r = new Result[futures.size()];
             Iterator<Future<Result<TestStatus>>> iter = futures.iterator();
             for (int j = 0; iter.hasNext(); j++)
                 r[j] = iter.next().get();
             return r;
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        finally {
            System.setIn(prevIn);
            System.setOut(prevOut);
            System.setErr(prevErr);
            resetDir();
            Runtime.getRuntime().removeShutdownHook(dirResetter);
        }
    }

    private static void resetDir() {
        changeDirectory(initDir);
    }

    private void removeTempDir() throws IOException {
        if (tempDir == null)
            return;
        Files.walk(tempDir)
            .sorted(Comparator.comparing(Path::getNameCount))
            .forEach(FunctionUtils.unchecked(Files::delete));
    }

    private static void changeDirectory(Path path) {
        nativeLoaderLock.lock();
        try {
            if (!triedLoadNative) {
                Path tempDir = null;
                Path tempFile = null;
                try {
                    tempDir = Files.createTempDirectory(null);
                    tempFile = tempDir.resolve(System.mapLibraryName("native"));
                    Files.copy(Objects.requireNonNull(BookChecker.class.getResourceAsStream("/native/"
                        + System.getProperty("os.arch") + "/"
                        + System.mapLibraryName("native"))), tempFile);
                    System.load(tempFile.toAbsolutePath().toString());
                    loadedNativeSuccess = true;
                }
                catch (UnsatisfiedLinkError | IOException | NullPointerException e) {
                    loadedNativeSuccess = false;
                    System.getLogger(BookChecker.class.getName()).log(System.Logger.Level.WARNING,
                        "Failed to load native library for " + System.getProperty("os.name")
                            + " on " + System.getProperty("os.arch"),
                        e);
                }
                finally {
                    try {
                        if (tempFile != null)
                            Files.deleteIfExists(tempFile);
                        if (tempDir != null)
                            Files.deleteIfExists(tempDir);
                    }
                    catch (IOException e) {
                        System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.DEBUG, "Error deleting temp file for native library", e);
                    }
                }
            }
        }
        finally {
            triedLoadNative = true;
            nativeLoaderLock.unlock();
        }
        if (loadedNativeSuccess) {
            try {
                changeDirectory(path.toString());
            }
            catch (UnsatisfiedLinkError e) {
                loadedNativeSuccess = false;
                System.getLogger(BookChecker.class.getName()).log(System.Logger.Level.WARNING,
                    "Failed to load native library for " + System.getProperty("os.name")
                        + " on " + System.getProperty("os.arch"),
                    e);
            }
        }
    }

    private static native void changeDirectory(String path);

    public boolean validateResults() throws IOException {
        if (isInvalidated)
            return false;

        if (document == null)
            return true;

        MessageDigest oldDigest, newDigest;

        try {
            oldDigest = (MessageDigest) digest.clone();
            newDigest = digests(1)[0];
        }
        catch (CloneNotSupportedException e) {
            oldDigest = digest;
            MessageDigest[] digests = digests(2);
            digest = digests[0];
            newDigest = digests[1];
        }

        return !(isInvalidated = !MessageDigest.isEqual(oldDigest.digest(), newDigest.digest()));
    }

    private MessageDigest[] digests(int count) throws IOException {
        MessageDigest[] digests = new MessageDigest[count];
        try {
            for (MessageDigest digest : digests)
                digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        if (document != null) {
            for (MessageDigest digest : digests)
                digest.update(book.getInputStream().readAllBytes());
        }

        if (tempDir != null) {
            Path newTempDir = Files.createTempDirectory(null);
            try {
                book.loadResources(newTempDir, getResources());
            }
            catch (ClassNotFoundException | SAXException | NoSuchMethodException e) {
                throw new IllegalStateException("getResources() should not throw exception if document is loaded");
            }
            OutputStream os = OutputStream.nullOutputStream();
            for (MessageDigest digest : digests)
                os = new DigestOutputStream(os, digest);
            try (ZipOutputStream out = new ZipOutputStream(os)) {
                Files.walk(tempDir)
                    .forEachOrdered(FunctionUtils.unchecked((ConsumerThrowsException<Path>) p -> {
                        String name = tempDir.relativize(p).toString();
                        if (Files.isDirectory(p))
                            name += '/';
                        out.putNextEntry(new ZipEntry(name));
                        if (!Files.isDirectory(p))
                            Files.copy(p, out);
                    }));
            }
        }
        return digests;
    }

    class CheckableType<T extends Annotation> {
        private final String name;
        private final Class<T> annotation;
        private final Method value;

        /**
         * Constructs a {@code CheckableType} from an {@code Element}
         *
         * @param e The element to construct from
         * @throws ClassNotFoundException If the class linked in the element could not be found.
         * @throws ClassCastException If the class linked in the element is not an annotation, or
         *          if the {@code value()} method does not return an int.
         * @throws NoSuchMethodException If the annotation does not have a {@code value()} method.
         */
        public CheckableType(Element e) throws ClassNotFoundException, NoSuchMethodException {
            this.name = e.getAttribute("name");
            Class<?> annotation = classLoader.loadClass(e.getAttribute("annotation"));
            if (!Annotation.class.isAssignableFrom(annotation))
                throw new ClassCastException("Cannot convert " + annotation.getName() + " to java.lang.Annotation");
            value = annotation.getMethod("value");
            if (!value.getReturnType().equals(int.class))
                throw new ClassCastException("Cannot convert " + value.getGenericReturnType() + " to int");
            value.trySetAccessible();
            this.annotation = (Class<T>) annotation;
        }

        public int value(T target) throws InvocationTargetException, IllegalAccessException {
            System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.TRACE, "Getting %s of %s", name, target);
            return (int) value.invoke(target);
        }

        public String name() {
            return name;
        }

        public Class<T> annotation() {
            return annotation;
        }
    }
}
