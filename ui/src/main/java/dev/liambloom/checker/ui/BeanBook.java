package dev.liambloom.checker.ui;

import dev.liambloom.checker.*;
import dev.liambloom.checker.internal.Util;
import dev.liambloom.util.function.FunctionUtils;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.fxml.FXML;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

public class BeanBook {
    public static final long RESULT_VALIDATION_PERIOD = 10_000;
    private static final AtomicInteger anonCount = new AtomicInteger(0);
    private static final Timer timer = new Timer(true);
    public final ObjectProperty<URLBook> inner = new SimpleObjectProperty<>() {
        @Override
        public URLBook get() {
            return super.get();
        }
    };
    public final ObjectBinding<BookReader> reader = new ObjectBinding<>() {
        { bind(BeanBook.this.inner); }

        @Override
        protected BookReader computeValue() {
            return new BookReader(BeanBook.this.inner.get());
        }
    };
    public final StringProperty name = new StringPropertyBase() {
        @Override
        public Object getBean() {
            return null;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public void set(String newValue) {
            String oldValue = get();
            if (Books.prefs.get(newValue, null) != null)
                throw new IllegalArgumentException("Book `" + newValue + "' already exists");
            String val = Books.prefs.get(oldValue, null);
            if (val == null){
                System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.TRACE, "\"%s\" doesn't exist", oldValue);
                return;
            }
            super.set(newValue);
            Books.prefs.remove(oldValue);
            Books.prefs.put(newValue, val);
            Books.loadedBooks.put(newValue, Books.loadedBooks.remove(oldValue));
        }
    };
    public final ObjectProperty<URL> url = new <>();
    public final ObjectBinding<Result<TestValidationStatus>> validationResult = new ObjectBinding<>() {
        { bind(BeanBook.this.inner); }

        @Override
        protected Result<TestValidationStatus> computeValue() {
            try {
                return reader.get().validateBook();
            }
            catch (IOException e) {
                System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.ERROR, "Error validating book", e);
                throw new UncheckedIOException(e);
            }
        }
    };
    public final BooleanBinding exists = new BooleanBinding() {
        { bind(inner); }

        @Override
        protected boolean computeValue() {
            try {
                return inner.get().exists();
            }
            catch (IOException e) {
                System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.ERROR, "Error check if book exists", e);
                throw new UncheckedIOException(e);
            }
        }
    };
    private final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            try {
                if (reader.isValid() && !reader.get().validateResults())
                    reader.invalidate(); // TODO: Check if this propagates
            }
            catch (IOException e) {
                System.getLogger(Util.generateLoggerName()).log(System.Logger.Level.ERROR, "Error validating results", e);
            }
        }
    };

    /*BeanBook(URLBook inner) {
        this("<anonymous book #" + anonCount.getAndIncrement() + ">", inner);
    }*/

    BeanBook(String name, URLBook inner) {
        this.inner.set(inner);
        this.name.set(name);
        this.url.set(inner.getUrl());
        timer.scheduleAtFixedRate(timerTask, 0, RESULT_VALIDATION_PERIOD);
        this.name.addListener((observable, oldValue, newValue) -> {
            System.getLogger(Long.toString(System.identityHashCode(BeanBook.this))).log(System.Logger.Level.TRACE, "Renaming \"%s\" -> \"%s\"", oldValue, newValue);

        });
        this.url.addListener(((observable, oldValue, newValue) -> {
            Books.move(name, newValue);
            this.inner.set(new URLBook(newValue));
        }));
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public URL getUrl() {
        return url.get();
    }

    public void setUrl(URL value) {
        url.set(value);
    }

    public ObjectProperty<URL> urlProperty() {
        return url;
    }

    public Result<TestValidationStatus> getValidationResult() {
        return validationResult.get();
    }

    public ObjectBinding<Result<TestValidationStatus>> validationResultProperty() {
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
    public Book getInnerBook() {
        return inner.get();
    }

    /**
     * Note: Future calls to this object will produce undefined results. Keep
     * in mind that no more than one instance of this book can exist at a time,
     *
     */
    void remove() {
        timerTask.cancel();
        inner.set(null);
    }
}
