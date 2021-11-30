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
    private final AtomicBoolean isRemoved = new AtomicBoolean(false);
    private final ObjectProperty<URLBook> inner = new SimpleObjectProperty<>() {
        @Override
        public URLBook get() {
            return super.get();
        }
    };
    private final ObjectBinding<BookReader> reader = new ObjectBinding<>() {
        { bind(BeanBook.this.inner); }

        @Override
        protected BookReader computeValue() {
            return new BookReader(BeanBook.this.inner.get());
        }
    };
    private final StringProperty name = new SimpleStringProperty();
    private final ObjectProperty<URL> url = new SimpleObjectProperty<>();
    private final ObjectBinding<Result<TestValidationStatus>> validationResult = new ObjectBinding<>() {
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
    private final BooleanBinding exists = new BooleanBinding() {
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

    public BeanBook(URLBook inner) {
        this("<anonymous book #" + anonCount.getAndIncrement() + ">", inner);
    }

    public BeanBook(String name, URLBook inner) {
        this.inner.set(inner);
        this.name.set(name);
        this.url.set(inner.getUrl());
        timer.scheduleAtFixedRate(timerTask, 0, RESULT_VALIDATION_PERIOD);
        this.name.addListener((observable, oldValue, newValue) -> {
            Books.rename(oldValue, newValue);
        });
        this.url.addListener(((observable, oldValue, newValue) -> {
            Books.prefBooks.put(name, newValue.toString());
            this.inner.set(new URLBook(newValue));
        }));
    }

    public String getName() {
        removedCheck();
        return name.get();
    }

    public void setName(String value) {
        removedCheck();
        name.set(value);
    }

    public StringProperty nameProperty() {
        removedCheck();
        return name;
    }

    public URL getUrl() {
        removedCheck();
        return url.get();
    }

    public void setUrl(URL value) {
        removedCheck();
        url.set(value);
    }

    public ObjectProperty<URL> urlProperty() {
        removedCheck();
        return url;
    }

    public Result<TestValidationStatus> getValidationResult() {
        removedCheck();
        return validationResult.get();
    }

    public ObjectBinding<Result<TestValidationStatus>> validationResultProperty() {
        removedCheck();
        return validationResult;
    }

    public boolean getExists() {
        removedCheck();
        return exists.get();
    }

    public BooleanBinding existsProperty() {
        removedCheck();
        return exists;
    }

    /**
     * WARNING: Do not save this value; only use it when this needs to be passed into a book
     * that takes a {@code Book}.
     *
     * @return The {@code Book} used internally by this {@code BeanBook}
     */
    public Book getInnerBook() {
        removedCheck();
        return inner.get();
    }

    void remove() {
        removedCheck();
        timerTask.cancel();
        isRemoved.setRelease(true);
        inner.set(null);
    }

    private void removedCheck() {
        if (isRemoved.getAcquire())
            throw new IllegalStateException("BeanBook " + name + " has been removed");
    }
}
