package dev.liambloom.checker.book;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.WatchEvent;
import java.util.function.Consumer;

@SupportedProtocols("jar")
public class JarURLWatcher implements URLWatcher {
    @Override
    public void addWatcher(URL url, Consumer<WatchEvent<?>> watcher) {

    }

    @Override
    public boolean removeWatcher(URL url, Consumer<WatchEvent<?>> watcher) {
        return false;
    }
}
