package dev.liambloom.checker.books;

import java.net.URI;

public record Meta(CheckableType<?> chapterType,
                   CheckableType<?>[] checkableTypes,
                   ClassLoader classLoader,
                   URI[] resources) {
}
