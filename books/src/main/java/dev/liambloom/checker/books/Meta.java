package dev.liambloom.checker.books;

import java.net.URL;

public record Meta(CheckableType<?> chapterType, CheckableType<?>[] checkableTypes, ClassLoader classLoader, URL[] resources) {
}
