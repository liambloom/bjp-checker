package dev.liambloom.checker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class URLBook extends BookLocator {
    private final URL url;

    public URLBook(URL url) {
        this.url = url;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return url.openStream();
    }

    @Override
    public boolean exists() throws IOException {
        try {
            url.openConnection().connect();
            return true;
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    protected Path loadResources(Path destination, String[] paths) throws IOException {
        // Obviously, I should create a temp file, but what do I do if the file changes midway through
        //  a test?
        // TODO
        Set<URL> urls = new HashSet<>();
        for (String p : paths)
            urls.add(new URL(url, p));
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
        base = base.substring(0, base.lastIndexOf('/') +  1);
        URL baseURL = new URL(url.getProtocol(), url.getHost(), url.getPort(), base);
        URI baseURI;
        try {
            baseURI = baseURL.toURI();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        for (URL url : urls) {
            Path d;
            try {
                d = destination.resolve(Path.of(baseURI.relativize(url.toURI()).toString()));
            }
            catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            assert !d.startsWith(destination);
            Files.createDirectories(d.getParent());
            Files.createFile(d);
            try (InputStream stream = url.openStream()) {
                Files.write(d, stream.readAllBytes());
            }
            catch (FileNotFoundException ignored) {}
        }

        return destination.resolve(baseURI.toString());
    }

    public URI getResourceBaseURI() throws URISyntaxException {
        return url.toURI();
    }

    @Override
    public boolean supportsResourceLoading() {
        return true;
    }

    public URL getUrl() {
        return url;
    }
}
