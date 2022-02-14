package dev.liambloom.checker.books;

import java.net.URI;
import java.util.Map;

public interface BookLocator extends ResourceLocator {
    String getName();
    Map<URI, ResourceLocator> getResources();
    URI getResourceBaseURI();
}
