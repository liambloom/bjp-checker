package dev.liambloom.checker.book;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;

public interface URLWatcher {
    void addWatcher(URL url, Consumer<WatchEvent<?>> watcher) throws IOException, URISyntaxException;
    boolean removeWatcher(URL url, Consumer<WatchEvent<?>> watcher);
}
