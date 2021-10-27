package dev.liambloom.checker.book;

import dev.liambloom.util.function.ConsumerThrowsException;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;

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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class URLBook implements Book {
    private final URL url;

    public URLBook(URL url) {
        this.url = url;
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
    public Path loadResources(Path destination, Stream<String> paths) throws IOException {
        // Obviously, I should create a temp file, but what do I do if the file changes midway through
        //  a test?
        // TODO
        Set<URL> urls = paths
            .map(FunctionUtils.unchecked((FunctionThrowsException<String, URL>) p -> new URL(url, p)))
            .collect(Collectors.toSet());
        if (urls.isEmpty())
            return destination;
        String base = urls.stream()
            .map(URL::getFile)
            .reduce((a, b) -> {
                int l = Math.min(a.length(), b.length());
                for (int i = 0; i < l; i++) {
                    if (a.charAt(i) != b.charAt(i))
                        return a.substring(0, i);
                }
                return a.length() == l ? a : b;
            })
            .get();
        base = base.substring(base.lastIndexOf('/'));
        urls.stream()
            .forEach(FunctionUtils.unchecked((ConsumerThrowsException<URL>) url -> {

            }));

        return ur

//        return Files.write(Files.createFile(p),
//            new URL(url, p.toString().replace(File.separatorChar, '/')).openStream().readAllBytes());
    }

    @Override
    public boolean supportsResourceLoading() {
        return true;
    }
}
