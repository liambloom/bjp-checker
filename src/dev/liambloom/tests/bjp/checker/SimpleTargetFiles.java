package dev.liambloom.tests.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleTargetFiles implements TargetFiles{
    private final Collection<Path> files;

    public SimpleTargetFiles() {
        this(new ArrayList<>());
    }

    public SimpleTargetFiles(Collection<Path> files) {
        this.files = files;
    }

    public void addDir(Path p) throws IOException {
        files.addAll(Files.walk(p, FileVisitOption.FOLLOW_LINKS)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList()));
    }

    @Override
    public Stream<Path> files() {
        if (files.isEmpty())
            throw new UserErrorException("Expected file, but no files were found");
        else
            return files.parallelStream();
    }

    @Override
    public Path single() {
        if (files.size() > 1)
            throw new UserErrorException("Expected single file, but multiple files were selected");
        else if (files.isEmpty())
            throw new UserErrorException("Expected file, but no files were found");
        else
            return files.iterator().next();
    }
}
