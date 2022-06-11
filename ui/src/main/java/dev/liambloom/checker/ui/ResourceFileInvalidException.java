package dev.liambloom.checker.ui;

public class ResourceFileInvalidException extends Exception {
    public ResourceFileInvalidException(String name) {
        super("Resource " + name + " invalid");
    }
}
