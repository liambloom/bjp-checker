package dev.liambloom.checker.books;

import java.net.URI;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record Meta(CheckableType<?> chapterType,
                   CheckableType<?>[] checkableTypes,
                   URLClassLoader classLoader,
                   URI[] resources) {

    public Set<String> checkableTypeNameSet() {
        return Arrays.stream(checkableTypes()).map(CheckableType::name).collect(Collectors.toSet());
    }
}
