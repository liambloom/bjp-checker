package dev.liambloom.checker.books;

import java.util.Collections;
import java.util.Set;

public record ClassConditions(Set<StaticExecutableTestInfo> methods/*,
                             Set<>*/) {
    public ClassConditions(Set<StaticExecutableTestInfo> methods) {
        this.methods = Collections.unmodifiableSet(methods);
    }
}
