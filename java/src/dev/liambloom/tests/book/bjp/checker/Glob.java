package dev.liambloom.tests.book.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Glob {
    private final Piece[] pieces;
    final boolean isTestGlob;
    final Logger logger;

    public Glob(String[] s, boolean isTestGlob, Logger logger) throws IOException {
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

        public Piece(String s) throws IOException {
            raw = s;
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
            if (isTestGlob && segmentsList.get(0).equals("@tests")) {
                base = App.testBase();
                segmentsList.remove(0); // This is O(n) and I wish it weren't
                if (segmentsList.isEmpty())
                    segmentsList.add(".");
            }
            else if (s.startsWith("/"))
                base = new File(File.separator).getCanonicalFile();
            else
                base = new File(".").getCanonicalFile();
            segments = segmentsList.toArray(new String[0]);
        }

        public List<File> files() throws IOException  {
            List<File> r = Stream.concat(files(base, 0), isTestGlob && segments.length == 1 && !base.equals(App.testBase()) ? files(App.testBase(), 0) : Stream.empty())
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
                    if (segments.length > i + 1) {
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
                    List<String> literalParts = new ArrayList<>();
                    StringBuilder builder = new StringBuilder();
                    StringBuilder builderPart = new StringBuilder();
                    boolean isEscaped = false;
                    // FIXME: DRY
                    for (char c : segment.toCharArray()) {
                        if (isEscaped)
                            isEscaped = false;
                        else {
                            if (c == '\\') {
                                isEscaped = true;
                                continue;
                            }
                            else if (c == '?' || c == '*') {
                                if (builderPart.length() != 0) {
                                    literalParts.add(builderPart.toString());
                                    builder.append("\\Q")
                                            .append(builderPart)
                                            .append("\\E");
                                    builderPart = new StringBuilder();
                                }
                                else if (builder.length() == 0)
                                    literalParts.add("");
                                builder.append("([^/]");
                                if (c == '*')
                                    builder.append(c);
                                builder.append(')');
                                continue;
                            }
                        }

                        if (c == 'E' && builderPart.length() != 0 && builderPart.charAt(builderPart.length() - 1) == '\\') {
                            // FIXME: I can't alternate literalParts and captures because of this.
                            literalParts.add(builderPart + "E");
                            builder.append("\\Q")
                                    .append(builderPart)
                                    .append("\\E")
                                    .append('E');
                            builderPart = new StringBuilder();
                        }
                        else
                            builderPart.append(c);
                    }
                    if (isEscaped)
                        throw new UserErrorException("Trailing backslash in glob \"" + raw + '"');
                    if (builderPart.length() != 0) {
                        literalParts.add(builderPart.toString());
                        builder.append("\\Q")
                                .append(builderPart)
                                .append("\\E");
                    }
                    if (isTestGlob)
                        builder.append("(?:\\.xml)?");
                    // FIXME: Some file systems are case sensitive, others are not
                    final Pattern p = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
                    Stream<File> r = Arrays.stream(base.listFiles((_file, name) -> {
                        Matcher matcher = p.matcher(name);
                        if (!matcher.matches())
                            return false;
                        int j, k;
                        for (j = 0, k = 1; j < literalParts.size() || ; j++, k++) {

                        }
                    }));
                    if (segments.length > i + 1){
                        try {
                            r = r.flatMap((FunctionThrowsIOException<File, Stream<File>>) (f -> files(f, i + 1)));
                        }
                        catch (UncheckedIOException e) {
                            throw e.getCause();
                        }
                    }
                    return r;
            }


        }

        /*private class Segment {
            //public final Pattern pattern;

            public
        }

        private class SegmentBuilder {
            private Optional<StringBuilder> builderPart = Optional.empty();
            private StringBuilder builder = new StringBuilder();
            private List<String> literalSegments = new ArrayList<>();

            public SegmentBuilder(String segment) {

            }
        }*/

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
                throw e.getCause();
            }
        }

        @Override
        public String toString() {
            return raw + " -> " + base + File.separator + String.join(File.separator, segments);
        }
    }

    public Stream<File> files() throws IOException {
        try {
            return Arrays.stream(pieces)
                    .unordered()
                    .parallel()
                    .map((FunctionThrowsIOException<Piece, List<File>>) Piece::files)
                    .flatMap(List::stream)
                    .distinct()
                    .sorted();
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public File single() throws IOException {
        return files().collect(Collectors.collectingAndThen(Collectors.toList(),
            list -> {
                if (list.size() != 1)
                    throw new UserErrorException("Expected single file, but glob matched " + list.size());
                return list.get(0);
            }
        ));
    }

    public static File readSymbolicLink(File file) throws IOException {
        return readSymbolicLink(file.toPath()).toFile();
    }

    public static Path readSymbolicLink(Path path) throws IOException {
        while (Files.isSymbolicLink(path))
            path = Files.readSymbolicLink(path);
        if (!path.toFile().exists())
            throw new UserErrorException("There was a symbolic link to `" + path + "', which doesn't exist.");
        return path;
    }
}
