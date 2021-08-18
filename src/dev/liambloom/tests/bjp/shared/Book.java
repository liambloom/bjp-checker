package dev.liambloom.tests.bjp.shared;

import dev.liambloom.tests.bjp.cli.CLILogger;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static java.nio.file.StandardWatchEventKinds.*;

public abstract class Book {
    protected String name;

    public Book(String name) {
        this.name = name;
    }

    public Result validate() throws IOException {
        if (exists()) {
            Validator v = validatorPool.poll();
            ValidationErrorHandler handler;
            if (v == null) {
                v = getTestSchema().newValidator();
                handler = new ValidationErrorHandler();
            }
            else {
                handler = (ValidationErrorHandler) v.getErrorHandler();
                handler.reset();
                v.reset();
            }
            v.setErrorHandler(handler);

            try {
                v.validate(getSource());
            } catch (SAXException ignored) {
            }
            validatorPool.add(v);

            if (handler.getMaxErrorKind() == null)
                return new Result(getName(), TestValidationStatus.VALID);
            else if (handler.getMaxErrorKind() == LogKind.WARN)
                return new Result(getName(), TestValidationStatus.VALID_WITH_WARNINGS, Optional.of(handler.getLogs()));
            else
                return new Result(getName(), TestValidationStatus.INVALID, Optional.of(handler.getLogs()));
        }
        else
            return new Result(getName(), TestValidationStatus.NOT_FOUND);
    }
    public String getName() {
        return name;
    }
    public boolean exists() throws IOException {
        return loadedTests.containsKey(getName());
    }
    // This should probably also be not abstract
    public abstract Document getDocument(DocumentBuilder db) throws SAXException, IOException;
    protected abstract Source getSource() throws IOException;

    private static final Map<Path, Collection<Consumer<WatchEvent<Path>>>> watcherCallbacks = Collections.synchronizedMap(new HashMap<>());
    private static final Map<FileSystem, WatchService> watchers = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Path, Collection<Path>> watcherSymlinkTargets = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Book> loadedTests = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, String> LOCAL_TEST_NAMES;
    private static final Queue<Validator> validatorPool = new ConcurrentLinkedQueue<>();
    private static Schema testSchema = null;
    private static Preferences customTests;

    static {
        LOCAL_TEST_NAMES = new HashMap<>();
        LOCAL_TEST_NAMES.put("BJP 3", "bjp3");
    }

    public static Schema getTestSchema() {
        if (testSchema == null) {
            try {
                SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
                factory.setFeature("http://apache.org/xml/features/validation/cta-full-xpath-checking", true);
                // TODO: factory.setErrorHandler(ErrorHandler)
                testSchema = factory.newSchema(
                        new StreamSource(App.class.getResourceAsStream("/book-tests.xsd")));
            }
            catch (SAXException e) {
                throw new RuntimeException(e);
            }
        }
        return testSchema;
    }

    public static Preferences getCustomTests() {
        if (customTests == null)
            customTests = App.prefs().node("tests");
        return customTests;
    }

    public static Book getTest(String name) {
        { // Get book if it has already been loaded
            Book alreadyLoaded = loadedTests.get(name);
            if (alreadyLoaded != null)
                return alreadyLoaded;
        }

        Book r;

        InputStream stream = Book.class.getClassLoader().getResourceAsStream("tests/" + LOCAL_TEST_NAMES.get(name) + ".xml");
        if (stream != null) { // Get test from jar
            r = new StreamBook(name, stream);
        }
        else { // Get tests from preferences
            String pathString = App.prefs().node("tests").get(name, null);

            if (pathString == null)
                throw new UserErrorException("Test \"" + name + "\" not found");

            Path p = Path.of(pathString);

            r = new PathBook(name, p);
        }


        loadedTests.put(name, r);
        return r;
    }

    public static boolean testExists(String name) {
        return loadedTests.containsKey(name)
                || Optional.ofNullable(LOCAL_TEST_NAMES.get(name)).map(Book.class.getClassLoader()::getResource).isPresent()
                || App.prefs().node("tests").get(name, null) != null;
    }

    public static void addTest(String name, Path p) throws IOException {
        if (testExists(name))
            throw new UserErrorException("Test `" + name + "' already exists");
        if (name.equals("-a") || name.equals("--all"))
            throw new UserErrorException("Invalid test name: `-a' and `--all' are reserved");
        if (name.length() > Preferences.MAX_KEY_LENGTH)
            throw new UserErrorException("Invalid test name: must not have a length greater than " + Preferences.MAX_KEY_LENGTH);
        if (!p.toString().endsWith(".xml"))
            throw new UserErrorException("Invalid test path: " + p + " is not xml");
        if (Files.notExists(p))
            throw new UserErrorException("Invalid test path: " + p + " does not exist");
        /*if (!Files.exists(p) || !p.toRealPath().toString().endsWith(".xml"))
            throw new UserErrorException("Path `" + p + "' is not xml");
        getCustomTests().put(name, p.toString());*/
        PathBook.setPath(name, p);
        Preferences index = getCustomTests().node("index");
        int size = index.getInt("size", 0);
        index.put(Integer.toString(size), name);
        index.putInt("size", size + 1);
    }

    public static void removeTest(String name) {
        loadedTests.remove(name);
        if (getCustomTests().get(name, null) == null)
            throw new UserErrorException("Test \"" + name + "\" not found");
        getCustomTests().remove(name);
        Preferences index = getCustomTests().node("index");
        int size = index.getInt("size", 0);
        int i;
        for (i = 0; i < size; i++) {
            if (index.get(Integer.toString(i), null).equals(name)){
                index.remove(Integer.toString(i));
                break;
            }
        }
        if (i == size)
            throw new IllegalStateException("Test \"" + name + "\" not found in index");
        for (; i < size - 1; i++)
            index.put(Integer.toString(i), index.get(Integer.toString(i + 1), null));
        index.remove(Integer.toString(size - 1));
        index.putInt("size", size - 1);
    }

    protected static void renameLoadedTest(String oldName, String newName) {
        synchronized (loadedTests) {
            loadedTests.compute(newName, (k, v) -> {
                if (v != null)
                    throw new UserErrorException("Name already taken");
                return loadedTests.get(oldName);
            });
            loadedTests.remove(oldName);
        }
    }

    protected static void addWatcher(Path p, Consumer<WatchEvent<Path>> cb) throws IOException {
        try {
            WatchService watcher = watchers.computeIfAbsent(p.getFileSystem(), (FunctionThrowsIOException<FileSystem, WatchService>) fileSystem -> {
                WatchService fsWatcher = fileSystem.newWatchService();

                new Thread(() -> {
                    // FIXME: This isn't working
                    // Notes: Msot of the changes seem to happen to custom-tests.xml~, which I assume is
                    //         some kind of temp file created by intellij. More importantly, however, is
                    //         that it is getting change events for C:\Users\liamr\Documents\GitHub\bjp3-tests\custom-test__.xml
                    //         which does not exist and is not (as far as I know) being watched. Either
                    //         intellij is doing something weird and "helpful", or my code is *very* broken.
                    while (true) {
                        WatchKey key;
                        try {
                            key = fsWatcher.take();
                        }
                        catch (InterruptedException e) {
                            App.logger.log(LogKind.ERROR, e.toString());
                            throw new RuntimeException(e);
                        }

                        for (WatchEvent<?> eventUnfiltered : key.pollEvents()) {
                            if (eventUnfiltered.kind() == OVERFLOW)
                                continue;

                            @SuppressWarnings("unchecked")
                            WatchEvent<Path> event = (WatchEvent<Path>) eventUnfiltered;
                            Path target = ((Path) key.watchable()).resolve(event.context());//.toAbsolutePath().normalize();
                            Queue<Path> targets = new LinkedList<>();
                            targets.add(target);

                            while (Files.isSymbolicLink(target)) {
                                Path targetTarget;
                                try {
                                    targetTarget = Files.readSymbolicLink(target);
                                }
                                catch (IOException e) {
                                    App.logger.log(LogKind.ERROR, "No longer watching for changes in tests. Reason: " + e.getMessage());
                                    return;
                                }
                                if (!watcherSymlinkTargets
                                        .computeIfAbsent(targetTarget.toAbsolutePath().normalize(), (FunctionThrowsIOException<Path, Collection<Path>>) (e -> {
                                            e.getParent().register(fsWatcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                                            return Collections.synchronizedCollection(new LinkedList<>());
                                        }))
                                        .add(target.toAbsolutePath().normalize()))
                                    break;
                                target = targetTarget;
                            }

                            while (!targets.isEmpty()) {
                                Path currentTarget = targets.remove();

                                Collection<Consumer<WatchEvent<Path>>> callbacks = watcherCallbacks.get(currentTarget);

                                if (callbacks == null)
                                    continue;

                                for (Consumer<WatchEvent<Path>> callback : callbacks)
                                    callback.accept(event);

                                Iterator<Path> symlinks = Optional.ofNullable(watcherSymlinkTargets.get(currentTarget))
                                        .map(Collection::iterator)
                                        .orElseGet(Collections::emptyIterator);
                                while (symlinks.hasNext()) {
                                    Path symlink = symlinks.next();
                                    if (!Files.isSymbolicLink(symlink)){
                                        symlinks.remove();
                                        continue;
                                    }

                                    Path symlinkTarget;
                                    try {
                                        symlinkTarget = Files.readSymbolicLink(symlink).toAbsolutePath().normalize();
                                    }
                                    catch (IOException e) {
                                        App.logger.log(LogKind.ERROR, "No longer watching for changes in tests. Reason: " + e.getMessage());
                                        return;
                                    }

                                    if (!symlinkTarget.equals(currentTarget)) {
                                        symlinks.remove();
                                        continue;
                                    }
                                    targets.add(symlinkTarget);
                                }
                            }
                        }

                        if (!key.reset()) {
                            App.logger.log(LogKind.ERROR, "No longer watching for changes in tests. Reason: WatchKey is invalid");
                            break;
                        }
                    }
                })
                        .start();

                return fsWatcher;
            });

            watcherCallbacks
                    .computeIfAbsent(p.toAbsolutePath().normalize(), (FunctionThrowsIOException<Path, Collection<Consumer<WatchEvent<Path>>>>) (key -> {
                        key.getParent().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                        return Collections.synchronizedCollection(new LinkedList<>());
                    }))
                    .add(cb);

            while (Files.isSymbolicLink(p)) {
                Path target = Files.readSymbolicLink(p);
                if (!watcherSymlinkTargets
                        .computeIfAbsent(target.toAbsolutePath().normalize(), (FunctionThrowsIOException<Path, Collection<Path>>) (key -> {
                            key.getParent().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                            return Collections.synchronizedCollection(new LinkedList<>());
                        }))
                        .add(p.toAbsolutePath().normalize()))
                    break;
                p = target;
            }
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public static Stream<Book> getAllTests() {
        Preferences index = getCustomTests().node("index");

        return Stream.concat(
                LOCAL_TEST_NAMES.keySet().stream(),
                IntStream.range(0, index.getInt("size", 0))
                    .mapToObj(i -> index.get(Integer.toString(i), null))
                    .filter(Objects::nonNull)
                    /*.map(name -> getCustomTests().get(name, null))
                    .filter(Objects::nonNull)*/
        )
                .map(Book::getTest);
    }

    private class ValidationErrorHandler implements ErrorHandler {
        private ByteArrayOutputStream log;
        private CLILogger logger;
        private LogKind maxErrorKind;

        public ValidationErrorHandler() {
            reset();
        }

        @Override
        public void warning(SAXParseException exception) {
            if (maxErrorKind == null)
                maxErrorKind = LogKind.WARN;
            logger.log(LogKind.WARN, getMessage(exception));
        }

        @Override
        public void error(SAXParseException exception) {
            if (maxErrorKind != LogKind.ERROR)
                maxErrorKind = LogKind.ERROR;
            logger.log(LogKind.ERROR, getMessage(exception));
        }

        @Override
        public void fatalError(SAXParseException exception) {
            if (maxErrorKind != LogKind.ERROR)
                maxErrorKind = LogKind.ERROR;
            logger.log(LogKind.ERROR, getMessage(exception));
            // TODO: Maybe log something that indicates parsing stopped here?
        }

        private String getMessage(SAXParseException e) {
            StringBuilder message = new StringBuilder(e.getMessage());
            if (message.charAt(message.length() - 1) == '.')
                message.deleteCharAt(message.length() - 1);

            return message.append(" at ")
                    .append(Book.this instanceof PathBook pathBook ? pathBook.getPath() : getName())
                    .append(':')
                    .append(e.getLineNumber())
                    .append(':')
                    .append(e.getColumnNumber())
                    .toString();
        }

        public ByteArrayOutputStream getLogs() {
            return log;
        }

        public LogKind getMaxErrorKind() {
            return maxErrorKind;
        }

        public void reset() {
            log = new ByteArrayOutputStream();
            logger = new CLILogger(new PrintStream(log));
            maxErrorKind = null;
        }
    }
}
