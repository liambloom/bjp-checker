package dev.liambloom.checker.ui.gui;

//import dev.liambloom.checker.*;
import dev.liambloom.checker.books.*;
import dev.liambloom.checker.books.xmlBook.XMLBookParser;
import dev.liambloom.checker.ui.BookManager;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

public class BeanBook {
//    private Book inner;
//
//    public BeanBook(Book inner) {
//        super(null, null);
//        this.inner = inner;
//    }
//
//    public Chapter getChapter(int chapter) {
//        return inner.getChapter(chapter);
//    }
//
//    @Override
//    public Meta getMeta() {
//        return inner.getMeta();
//    }
//
//    @Override
//    public BookLocator getLocator() {
//        return inner.getLocator();
//    }


    public static final long RESULT_VALIDATION_PERIOD = 10_000;
    private static final AtomicInteger anonCount = new AtomicInteger(0);
    private static final Timer timer = new Timer(true);
    private final System.Logger logger = System.getLogger(BookManager.SelfLoadingBook.class.getName() + System.identityHashCode(this));
    private final ObjectBinding<BookLocator> inner;
    public final ReadOnlyStringProperty name;
    public final ReadOnlyObjectProperty<URL> url;
    private Book book;
    public final ObjectBinding<Result<BookValidationStatus>> validationResult;
    public final BooleanBinding exists;
//    private final TimerTask timerTask = new TimerTask() {
//        @Override
//        public void run() {
//            try {
//                if (reader.isValid() && !reader.get().validateResults())
//                    reader.invalidate(); // TODO: Check if this propagates
//            }
//            catch (IOException e) {
//                System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.ERROR, "Error validating results", e);
//            }
//        }
//    };

    /*BeanBook(URLBook inner) {
        this("<anonymous book #" + anonCount.getAndIncrement() + ">", inner);
    }*/

    BeanBook(String name, URL url) {
        this(new BookLocator(name, url));
    }

    BeanBook(BookLocator inner) {
        this.name = new StringPropertyBase(inner.name()) {
            @Override
            public Object getBean() {
                return null;
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public void setValue(String newValue) {
                String oldValue = get();
                if (Books.prefs.get(newValue, null) != null)
                    throw new IllegalArgumentException("Book `" + newValue + "' already exists");
                String val = Books.prefs.get(oldValue, null);
                if (val == null){
//                    logger.log(System.Logger.Level.TRACE, "\"%s\" doesn't exist", oldValue);
                    return;
                }
                super.set(newValue);
                Books.prefs.remove(oldValue);
                Books.prefs.put(newValue, val);
                Books.loadedBooks.put(newValue, Books.loadedBooks.remove(oldValue));
            }

            @Override
            public void set(String newValue) {
                setValue(newValue);
            }
        };
        this.url = new SimpleObjectProperty<>(inner.url());
        this.inner = new ObjectBinding<>() {
            { bind(name, url); }

            @Override
            protected BookLocator computeValue() {
                return new BookLocator(name.get(), url.get());
            }
        };
        validationResult = new ObjectBinding<>() {
            { bind(BookManager.SelfLoadingBook.this.inner); }

            @Override
            protected Result<BookValidationStatus> computeValue() {
                try {
                    return new XMLBookParser().validate(BookManager.SelfLoadingBook.this.inner.get());
                }
                catch (IOException e) {
                    logger.log(System.Logger.Level.ERROR, "Error validating book", e);
                    throw new UncheckedIOException(e);
                }
            }
        };

        exists = new BooleanBinding() {
            { bind(BookManager.SelfLoadingBook.this.inner); }

            @Override
            protected boolean computeValue() {
                try {
                    BookManager.SelfLoadingBook.this.inner.get().url().openConnection().connect();
                    return true;
                }
                catch (FileNotFoundException e) {
                    return false;
                }
                catch (IOException e) {
                    logger.log(System.Logger.Level.ERROR, "Error check if book exists", e);
                    throw new UncheckedIOException(e);
                }
            }
        };

        this.inner.addListener((observableValue, oldValue, newValue) -> book = null);
//        timer.scheduleAtFixedRate(timerTask, 0, RESULT_VALIDATION_PERIOD);
        /*this.url.addListener(((observable, oldValue, newValue) -> {
            Books.prefs.put(inner.name(), newValue.toString());
            try {
                this.inner.set(Books.newBook(this.name.get(), newValue));
            }
            catch (BookParserException | IOException | URISyntaxException | ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }));*/
    }

    public String getName() {
        return name.get();
    }

//    public void setName(String value) {
//        name.set(value);
//    }

//    public StringProperty nameProperty() {
//        return name;
//    }

    public URL getUrl() {
        return url.get();
    }

//    public void setUrl(URL value) {
//        url.set(value);
//    }

//    public ObjectProperty<URL> urlProperty() {
//        return url;
//    }

    public Result<BookValidationStatus> getValidationResult() {
        return validationResult.get();
    }

    public ObjectBinding<Result<BookValidationStatus>> validationResultProperty() {
        return validationResult;
    }

    public boolean getExists() {
        return exists.get();
    }

    public BooleanBinding existsProperty() {
        return exists;
    }

    /**
     * WARNING: Do not save this value; only use it when this needs to be passed into a book
     * that takes a {@code Book}.
     *
     * @return The {@code Book} used internally by this {@code BeanBook}
     */
    public BookLocator getBookLocator() {
        return inner.get();
    }

    public Book getBook() throws BookParserException, IOException, URISyntaxException, ClassNotFoundException, NoSuchMethodException {
        if (book == null)
            book = new XMLBookParser().getBook(inner.get());
        return book;
    }

    /**
     * Note: Future calls to this object will produce undefined results. Keep
     * in mind that no more than one instance of this book can exist at a time,
     *
     */
//    void remove() {
////        timerTask.cancel();
//        inner.set(null);
//    }
}
