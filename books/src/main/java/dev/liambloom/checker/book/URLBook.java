//package dev.liambloom.checker.book;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.nio.file.Path;
//
//public class URLBook extends AbstractBook {
//    private final URL url;
//
//    public URLBook(String name, URL url) {
//        super(name);
//        this.url = url;
//    }
//
//    @Override
//    public InputStream getInputStream() throws IOException {
//        return url.openStream();
//    }
//
//
//    @Override
//    public boolean exists() throws IOException {
//        try {
//            getInputStream();
//            return true;
//        }
//        catch (FileNotFoundException e) {
//            return false;
//        }
//    }
//
//    @Override
//    public Path resolve(Path p) throws MalformedURLException {
//        // Obviously, I should create a temp file, but what do I do if the file changes midway through
//        //  a test? Should check be moved to this?
//        new URL(url, p.toString().replace(File.separatorChar, '/'));
//    }
//
//    @Override
//    public boolean supportsFileResolution() {
//        return true;
//    }
//}
