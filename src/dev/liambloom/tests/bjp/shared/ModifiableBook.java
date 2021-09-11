package dev.liambloom.tests.bjp.shared;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;

public interface ModifiableBook extends Book {
    void rename(String name);

    void addWatcher(Consumer<WatchEvent<Path>> cb) throws IOException;

    void removeWatcher(Consumer<WatchEvent<Path>> cb);
}
