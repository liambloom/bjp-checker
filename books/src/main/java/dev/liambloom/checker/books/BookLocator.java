package dev.liambloom.checker.books;

import java.net.URI;
import java.net.URL;
import java.util.Map;

public interface BookLocator {
    String getName();
//    URI[] getResources();
//    URI getResourceBaseURI();
    URL getURL();
}
