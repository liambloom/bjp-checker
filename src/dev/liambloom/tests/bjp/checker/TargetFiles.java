package dev.liambloom.tests.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

public interface TargetFiles {
    Stream<File> files() throws IOException;
    File single() throws IOException;
}
