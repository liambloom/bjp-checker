package dev.liambloom.checker.books;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record StaticExecutableTestInfo(TargetLocator locator,
                                       Conditions conditions) {

    public enum Type {
        METHOD,
        CONSTRUCTOR,
        PROGRAM
    }

    public record Conditions(InputStream in,
                             Object[] args,
                             Post[] argConditions,
                             String expectedOut,
                             Post expectedReturns,
                             Class<? extends Throwable> expectedThrows,
                             Map<Path, String> writesTo) {  }

    public record TargetLocator(Type type,
                                Optional<String> name,
                                Optional<Class<?>> container,
                                Class<?>[] params) { }
}
