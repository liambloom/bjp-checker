package dev.liambloom.checker.ui;

import dev.liambloom.checker.NotYetImplementedError;
import dev.liambloom.checker.URLBook;
import dev.liambloom.checker.internal.Util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class Books {
    static final Map<String, BeanBook> loadedBooks = new WeakHashMap<>();
    static final Preferences prefs = Preferences.userNodeForPackage(Books.class).node("books");
    private static final AtomicInteger anonCount = new AtomicInteger(1);

    private Books() {
    }

    /**
     * Gets a book. No more than 1 instance of a particular book can exist at once, if
     * repeated calls are made for the same book, the same instance may be returned. If
     * a strong pointer is not kept to the book, and it is cleaned up by the garbage
     * collector, this method may construct a new instance.
     *
     * @param name The name of the book to be retrieved
     * @return The book
     * @throws NullPointerException If the book does not exist
     */
    public static BeanBook getBook(String name) {
        return loadedBooks.computeIfAbsent(name, key -> {
            String url = prefs.get(name, null);
            try {
                return new BeanBook(name, new URLBook(new URL(Objects.requireNonNull(url, "Book \"" + name + "\" does not exist"))));
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static BeanBook getAnonymousBook(URL url) {
        return new BeanBook("<anonymous book #" + anonCount.getAndIncrement() + ">", new URLBook(url));
    }

    public static String[] getAllBookNames() throws BackingStoreException {
        return prefs.keys();
    }

    public static BeanBook[] getAllBooks() throws BackingStoreException {
        String[] names = getAllBookNames();
        BeanBook[] books = new BeanBook[names.length];
        for (int i = 0; i < books.length; i++)
            books[i] = getBook(names[i]);
        return books;
    }

    /*public static Optional<String> getDefaultBookName() {
        return Optional.ofNullable(prefs.get(DEFAULT_BOOK_KEY, null));
            //.map(Books::getBook);
    }*/

    /*public static Optional<BeanBook> getDefaultBook() {
        return getDefaultBookName().map(Books::getBook);
    }

    public static void setDefaultBook(String b) {
        if (b == null)
            prefs.remove(DEFAULT_BOOK_KEY);
        else if (prefs.get(b, null) != null)
            prefs.put(DEFAULT_BOOK_KEY, b);
        else
            throw new NullPointerException("Book \"" + b + "\" does not exist");
    }*/

    public static void add(String s, URL url) {
        if (prefs.get(s, null) != null)
            throw new IllegalArgumentException("Book `" + s + "' already exists");
        prefs.put(s, url.toString());
    }

    static void rename(String oldValue, String newValue) {
        System.Logger logger = System.getLogger(Util.generateLoggerName());
        logger.log(System.Logger.Level.TRACE, "Renaming in prefs \"%s\" -> \"%s\"", oldValue, newValue);

    }

    static void move(String name, URL target) {
        prefs.put(name, target.toString());
    }

    public static void remove(String name) {
        if (prefs.get(name, null) == null)
            throw new IllegalArgumentException("Book \"" + name + "\" doesn't exist");
        prefs.remove(name);
        Optional.ofNullable(loadedBooks.remove(name)).ifPresent(BeanBook::remove);
    }

//    public static boolean existsNamed(String n) {
//        return prefBooks.get(n, null) != null;
//    }

//    private static final Map<String, Book> loadedTests = Collections.synchronizedMap(new HashMap<>());
//    private static final Map<String, String> LOCAL_TEST_NAMES;
//    private static final Lazy<Preferences> customTests = new Lazy<>(() -> App.prefs().node("tests"));



//    static {
//        LOCAL_TEST_NAMES = new HashMap<>();
//        LOCAL_TEST_NAMES.put("BJP 3", "bjp3");
//    }

//    public static Preferences getCustomTests() {
//        return customTests.get();
//    }

//    public static Book getBook(String name) {
//        { // Get book if it has already been loaded
//            Book alreadyLoaded = loadedTests.get(name);
//            if (alreadyLoaded != null)
//                return alreadyLoaded;
//        }
//
//        Book r;
//
//        /*InputStream stream = Book.class.getClassLoader().getResourceAsStream("tests/" + LOCAL_TEST_NAMES.get(name) + ".xml");
//        if (stream != null) // Get test from jar
//            r = new StreamBook(name, stream);
//        else { // Get tests from preferences
//            String pathString = App.prefs().node("tests").get(name, null);
//
//            if (pathString == null)
//                throw new UserErrorException("Test \"" + name + "\" not found");
//
//            Path p = Path.of(pathString);
//
//            r = new PathBook(name, p);
//        }*/
//
//
//        loadedTests.put(name, r);
//        return r;
//    }
//
//    public static boolean bookNameExists(String name) {
//        return loadedTests.containsKey(name)
//            || Optional.ofNullable(LOCAL_TEST_NAMES.get(name)).map(s -> "tests/" + s + ".xml").map(Book.class.getClassLoader()::getResource).isPresent()
//            || App.prefs().node("tests").get(name, null) != null;
//    }
//
//    public static void addBook(String name, Path p) throws IOException {
//        if (bookNameExists(name))
//            throw new IllegalArgumentException("Test `" + name + "' already exists");
//        if (name.equals("-a") || name.equals("--all"))
//            throw new IllegalArgumentException("Invalid test name: `-a' and `--all' are reserved");
//        if (name.length() > Preferences.MAX_KEY_LENGTH)
//            throw new IllegalArgumentException("Invalid test name: must not have a length greater than " + Preferences.MAX_KEY_LENGTH);
//        if (!p.toString().endsWith(".xml"))
//            throw new IllegalArgumentException("Invalid test path: " + p + " is not xml");
//        if (Files.notExists(p))
//            throw new IllegalArgumentException("Invalid test path: " + p + " does not exist");
//        /*if (!Files.exists(p) || !p.toRealPath().toString().endsWith(".xml"))
//            throw new UserErrorException("Path `" + p + "' is not xml");
//        getCustomTests().put(name, p.toString());*/
//        PathBook.setPath(name, p);
//        Preferences index = getCustomTests().node("index");
//        int size = index.getInt("size", 0);
//        index.put(Integer.toString(size), name);
//        index.putInt("size", size + 1);
//    }
//
//    public static void removeBook(String name) {
//        loadedTests.remove(name);
//        if (getCustomTests().get(name, null) == null)
//            throw new UserErrorException("Test \"" + name + "\" not found");
//        getCustomTests().remove(name);
//        Preferences index = getCustomTests().node("index");
//        int size = index.getInt("size", 0);
//        int i;
//        for (i = 0; i < size; i++) {
//            if (index.get(Integer.toString(i), null).equals(name)) {
//                index.remove(Integer.toString(i));
//                break;
//            }
//        }
//        if (i == size)
//            throw new IllegalStateException("Test \"" + name + "\" not found in index");
//        for (; i < size - 1; i++)
//            index.put(Integer.toString(i), index.get(Integer.toString(i + 1), null));
//        index.remove(Integer.toString(size - 1));
//        index.putInt("size", size - 1);
//    }
//
//    public static void updateLoadedBookName(String oldName) {
//        synchronized (loadedTests) {
//            Book book = loadedTests.remove(oldName);
//            if (book == null)
//                throw new IllegalArgumentException("Book \"" + oldName + "\" does not exist");
//            loadedTests.compute(book.getName(), (k, v) -> {
//                if (v != null)
//                    throw new UserErrorException("Name already taken");
//                return loadedTests.get(oldName);
//            });
//        }
//    }
//
//    public static Stream<Book> getAllBooks() {
//        Preferences index = getCustomTests().node("index");
//
//        return Stream.concat(
//            LOCAL_TEST_NAMES.keySet().stream(),
//            IntStream.range(0, index.getInt("size", 0))
//                .mapToObj(i -> index.get(Integer.toString(i), null))
//                .filter(Objects::nonNull)
//                    /*.map(name -> getCustomTests().get(name, null))
//                    .filter(Objects::nonNull)*/
//        )
//            .map(Books::getBook);
//    }
}
