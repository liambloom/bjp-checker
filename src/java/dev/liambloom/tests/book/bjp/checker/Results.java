package dev.liambloom.tests.book.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

public class Results {
    private Results() {}

    public void save(File tests, int chapter, Stream<Result> results) throws IOException {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tests.toPath()));
        } catch (NoSuchAlgorithmException ignored) {}
        StringBuilder builder = new StringBuilder(8);
    }

    public static Results loadFromFile() {
        // TODO
        return null;
    }
}
