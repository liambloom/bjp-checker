package dev.liambloom.tests.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface TargetFiles {
    Stream<Path> files() throws IOException;
    Path single() throws IOException;
}
