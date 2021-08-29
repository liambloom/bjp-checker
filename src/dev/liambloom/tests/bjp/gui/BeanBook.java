package dev.liambloom.tests.bjp.gui;

import dev.liambloom.tests.bjp.shared.*;
import javafx.beans.property.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Optional;

public class BeanBook {
    private final Book inner;
    public final StringProperty name;
    public final ObjectProperty<Optional<Path>> path;
//    private final ReadOnlyBooleanWrapper isValidWrapper;
//    public final ReadOnlyBooleanProperty isValid;
    private final ReadOnlyObjectWrapper<Result> validationResultWrapper = new ReadOnlyObjectWrapper<>();
    public final ReadOnlyObjectProperty<Result> validationResult = validationResultWrapper.getReadOnlyProperty();
    private final ReadOnlyBooleanWrapper existsWrapper = new ReadOnlyBooleanWrapper();
    public final ReadOnlyBooleanProperty exists = existsWrapper.getReadOnlyProperty();

    public BeanBook(Book inner) throws IOException {
        this.inner = inner;
        name = new SimpleStringProperty(inner.getName());
        name.addListener((observable, oldValue, newValue) -> {
            if (inner instanceof ModifiableBook mb)
                mb.rename(newValue);
            else
                throw new UnsupportedOperationException("Attempt to rename BeanBook that wraps an unmodifiable book");
        });
        path = new SimpleObjectProperty<>(inner instanceof PathBook pb ? Optional.of(pb.getPath()) : Optional.empty());
        path.addListener(((observable, oldValue, newValue) -> {
            if (inner instanceof PathBook pb) {
                try {
                    pb.setPath(newValue.orElseThrow());
                    onChange(null);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

            }
            else
                throw new UnsupportedOperationException("Attempt to change path of BeanBook that wraps a non-path book");
        }));
        onChange(null);
        if (inner instanceof ModifiableBook mb)
            mb.addWatcher((ConsumerThrowsIOException<WatchEvent<Path>>) this::onChange);
    }

    private void onChange(WatchEvent<Path> e) throws IOException {
        validationResultWrapper.set(inner.validate());
        existsWrapper.set(inner.exists());
    }

    public boolean isModifiable() {
        return inner instanceof ModifiableBook;
    }

    public boolean hasPath() {
        return inner instanceof PathBook;
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

    public Optional<Path> getPath() {
        return path.get();
    }

    public void setPath(Path value) {
        path.set(Optional.of(value));
    }

    public ObjectProperty<Optional<Path>> pathProperty() {
        return path;
    }

    public Result getValidationResult() {
        return validationResult.get();
    }

    public ReadOnlyObjectProperty<Result> validationResultProperty() {
        return validationResult;
    }

    public boolean getExists() {
        return exists.get();
    }

    public ReadOnlyBooleanProperty existsProperty() {
        return exists;
    }
}
