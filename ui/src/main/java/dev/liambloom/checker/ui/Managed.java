package dev.liambloom.checker.ui;

import java.net.URL;
import java.util.UUID;

public interface Managed {
    String name();
    UUID id();
    Digest[] digest();
    URL sourceUrl();
    boolean download();

    default String[] fields() {
        return new String[]{ "name", "id", "digest", "sourceUrl", "download" };
    }
}
