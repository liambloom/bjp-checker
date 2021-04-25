package dev.liambloom.tests.book.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Glob {
    private final Piece[] pieces;
    final boolean isTestGlob;
    final Logger logger;

    public Glob(String[] s, boolean isTestGlob, Logger logger) {
        if (s.length == 0)
            throw new UserErrorException("No glob found. For more information, run `check glob --help'");
        this.isTestGlob = isTestGlob;
        this.logger = logger;
        pieces = new Piece[s.length];
        for (int i = 0; i < s.length; i++)
            pieces[i] = new Piece(s[i]);
    }

    private class Piece {
        final File base;
        final String[] segments;
        final String raw;

        public Piece(String s) {
            raw = s;
            if (isTestGlob) {
                if (s.startsWith("/"))
                    throw new UserErrorException("Tests must be in the directory " + App.here + File.separator + "tests");
                base = new File(App.here, "tests");
            }
            else if (s.startsWith("/"))
                base = new File(File.separator);
            else
                base = new File("");
            boolean isEscaped = false;
            StringBuilder builder = null;
            List<String> segmentsList = new ArrayList<>();
            Stream<Character> chars = s.chars().mapToObj(i -> (char) i);
            if (s.startsWith("/"))
                chars = chars.skip(1);
            Iterator<Character> iter = chars.iterator();
            while (iter.hasNext()) {
                char c = iter.next();
                if (isEscaped)
                    isEscaped = false;
                else if (c == '/') {
                    if (builder == null)
                        throw new UserErrorException("Invalid Path: Path contained illegal sequence \"//\"");
                    segmentsList.add(builder.toString());
                    builder = null;
                    continue;
                }
                if (c == '\\')
                    isEscaped = true;
                if (builder == null)
                    builder = new StringBuilder();
                builder.append(c);
            }
            if (builder != null)
                segmentsList.add(builder.toString());
            segments = segmentsList.toArray(new String[0]);
            if (isTestGlob) {
                if (segments.length > 1)
                    throw new UserErrorException("Illegal Test Glob: Test globs may only have one segment");
                else if (segments[0].equals("..") || segments[0].equals("**"))
                    throw new UserErrorException("Illegal Test Glob: Globs \"..\" and \"**\" may not be used to point to tests");
            }
        }

        public List<File> files() throws IOException  {
            List<File> r = files(base, 0)
                    .collect(Collectors.toList());
            if (r.size() == 0)
                throw new UserErrorException("Glob \"" + raw + "\" did not match any files");
            return r;
        }

        private Stream<File> files(File base, int i) throws IOException {
            assert base.isDirectory();
            if (segments.length == i)
                return allFiles(base);
            final String segment = segments[i];
            switch (segment) {
                case ".":
                    return files(base, i + 1);
                case "..":
                    return files(base.getParentFile(), i + 1);
                case "**":
                    if (segments.length > i + 2) {
                        try {
                            return Stream.concat(
                                files(base, i + 1),
                                Arrays.stream(base.listFiles())
                                    .map((FunctionThrowsIOException<File, File>) Glob::readSymbolicLink)
                                    .filter(File::isDirectory)
                                    .flatMap((FunctionThrowsIOException<File, Stream<File>>) (dir -> files(dir, i)))
                            );
                        }
                        catch (UncheckedIOException e) {
                            throw e.getCause();
                        }
                    }
                    else
                        return allFiles(base);
                default:
                    if (segment.contains("**"))
                        logger.warn("\"**\" as part of a larger segment is interpreted as two single stars")
                                .notice("To use a double star, make it its own path segment");
                    final StringBuilder builder = new StringBuilder("\\Q");
                    boolean isEscaped = false;
                    for (char c : segment.toCharArray()) {
                        if (isEscaped)
                            builder.append(c);
                        else {
                            switch (c) {
                                case '\\':
                                    isEscaped = true;
                                    break;
                                case '*':
                                    builder.append("\\E[^/]*\\Q");
                                    break;
                                case '?':
                                    builder.append("\\E[^/]\\Q");
                                    break;
                                default:
                                    builder.append(c);
                            }
                        }
                    }
                    builder.append("\\E");
                    if (isTestGlob)
                        builder.append("(?:\\.xml)?");
                    final Pattern p = Pattern.compile(builder.toString());
                    return Arrays.stream(base.listFiles((_file, name) -> p.matcher(name).matches()));
            }
        }

        private Stream<File> allFiles(File base) throws IOException {
            assert base.isDirectory();
            final Stream.Builder<File> files = Stream.builder();
            final Stream.Builder<File> dirs = Stream.builder();
            for (File f : base.listFiles()) {
                f = readSymbolicLink(f);
                if (f.isFile()) {
                    files.accept(f);
                }
                else {
                    assert f.isDirectory();
                    dirs.accept(f);
                }
            }
            try {
                return Stream.concat(files.build(), dirs.build().flatMap((FunctionThrowsIOException<File, Stream<File>>) this::allFiles));
            }
            catch (UncheckedIOException e) {
                System.out.println("How?");
                throw e.getCause();
            }
        }
    }

    public Stream<File> files() throws IOException {
        try {
            return Arrays.stream(pieces)
                    .unordered()
                    .parallel()
                    .flatMap((FunctionThrowsIOException<Piece, Stream<File>>) (p -> p.files().stream()))
                    .distinct()
                    .sorted();
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public static File readSymbolicLink(File file) throws IOException {
        return readSymbolicLink(file.toPath()).toFile();
    }

    public static Path readSymbolicLink(Path path) throws IOException {
        while (Files.isSymbolicLink(path))
            path = Files.readSymbolicLink(path);
        return path;
    }
}
