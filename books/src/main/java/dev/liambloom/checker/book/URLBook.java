package dev.liambloom.checker.book;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class URLBook implements Book {
    private final URL url;
    private URLWatcher watcher = null;

    public URLBook(URL url) {
        this.url = url;
    }

    private URLWatcher getWatcher() throws UnknownServiceException {
        if (watcher == null) {
            watcher = ServiceLoader.load(URLWatcher.class).stream()
                .filter(s -> Arrays.asList(s.type().getAnnotation(SupportedProtocols.class).value()).contains(url.getProtocol()))
                .collect(Collectors.collectingAndThen(
                    Collectors.toSet(),
                    c -> {
                        if (c.size() > 1)
                            System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING
                                , "Multiple watchers exist for URL protocol " + url.getProtocol());
                        if (c.isEmpty())
                            return Optional.<URLWatcher>empty();
                        else
                            return Optional.of(c.iterator().next().get());
                    }))
                .orElseThrow(() ->
                    new UnknownServiceException("No URL watchers exist for the url protocol " + url.getProtocol()));
        }
        return watcher;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    @Override
    public boolean exists() throws IOException {
        try {
            getInputStream();
            return true;
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public Path loadResources(Stream<Path> p) throws IOException {
        // Obviously, I should create a temp file, but what do I do if the file changes midway through
        //  a test? Should check be moved to this?
        // TODO
//        return Files.write(Files.createFile(p),
//            new URL(url, p.toString().replace(File.separatorChar, '/')).openStream().readAllBytes());
    }

    @Override
    public boolean supportsResourceLoading() {
        return true;
    }

    @Override
    public void addWatcher(Consumer<WatchEvent<?>> cb) throws IOException, URISyntaxException {
        getWatcher().addWatcher(url, cb);
    }

    @Override
    public boolean removeWatcher(Consumer<WatchEvent<?>> cb) {
        if (watcher == null)
            return false;
        try {
            return getWatcher().removeWatcher(url, cb);
        }
        catch (UnknownServiceException ignored) {
            return false;
        }
    }
}
