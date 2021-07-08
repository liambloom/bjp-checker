package dev.liambloom.tests.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Glob {
    private final Piece[] pieces;
    final boolean isTestGlob;
    final Logger logger;

    public Glob(Collection<String> s, boolean isTestGlob, Logger logger) throws IOException {
        if (s.size() == 0)
            throw new UserErrorException("No glob found. For more information, run `check glob --help'");
        this.isTestGlob = isTestGlob;
        this.logger = logger;
        Iterator<String> iter = s.iterator();
        pieces = new Piece[s.size()];
        for (int i = 0; iter.hasNext(); i++)
            pieces[i] = new Piece(iter.next());
    }

    public Glob(String[] s, boolean isTestGlob, Logger logger) throws IOException {
        this(Arrays.asList(s), isTestGlob, logger);
    }

    private class Piece {
        final File base;
        final String[] segments;
        final String raw;
        final String src;

        public Piece(String s) throws IOException {
            this(s, s);
        }

        private Piece(String s, String src) throws IOException {
            this.src = src;
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

        public List<File> files() throws IOException {
            List<File> r = Stream.concat(files(base, 0), isTestGlob && segments.length == 1 && !base.equals(App.testBase()) ? files(App.testBase(), 0) : Stream.empty())
                    .collect(Collectors.toList());
            if (r.size() == 0) {
                if (raw.contains(File.separator) && !raw.contains("/")){
                    r = new Piece(raw.replace(File.separatorChar, '/'), src).files();
                    logger.warn("Using \"\\\" instead of \"/\" is slower and does not support escaping");
                }
                else {
                    starWarning();
                    throw new UserErrorException("Glob \"" + src + "\" did not match any files");
                }
            }
            else
                starWarning();

            return r;
        }

        private void starWarning() {
            if (Arrays.stream(segments).anyMatch(s -> !s.equals("**") && s.contains("**")))
                logger.warn("\"**\" as part of a larger segment is interpreted as two single stars");
        }

        private Stream<File> files(File base, int i) throws IOException {
            assert base.isDirectory();
            if (segments.length == i)
                // TODO: Check this. IDK if it properly handles links
                return Files.walk(base.toPath(), FileVisitOption.FOLLOW_LINKS)
                        .map(Path::toFile)
                        .filter(File::isFile);
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
                        return files(base, i + 1);
                default:
                    List<String> literalParts = new ArrayList<>();
                    StringBuilder patternBuilder = new StringBuilder();
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
                                    patternBuilder.append("\\Q")
                                            .append(builderPart)
                                            .append("\\E");
                                    builderPart = new StringBuilder();
                                }
                                patternBuilder.append("([^/]");
                                if (c == '*')
                                    patternBuilder.append(c);
                                patternBuilder.append(')');
                                literalParts.add(null);
                                continue;
                            }
                        }

                        if (c == 'E' && builderPart.length() != 0 && builderPart.charAt(builderPart.length() - 1) == '\\') {
                            // FIXME: I can't alternate literalParts and captures because of this.
                            literalParts.add(builderPart + "E");
                            patternBuilder.append("\\Q")
                                    .append(builderPart)
                                    .append("\\E")
                                    .append('E');
                            builderPart = new StringBuilder();
                        }
                        else
                            builderPart.append(c);
                    }
                    if (isEscaped)
                        throw new UserErrorException("Trailing backslash in glob \"" + src + '"');
                    if (builderPart.length() != 0) {
                        literalParts.add(builderPart.toString());
                        patternBuilder.append("\\Q")
                                .append(builderPart)
                                .append("\\E");
                    }
                    if (isTestGlob)
                        patternBuilder.append("(?:\\.xml)?");
                    // TODO: Test this
                    final Pattern patternCaseSensitive = Pattern.compile(patternBuilder.toString());
                    final Pattern patternCaseInsensitive = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
                    @SuppressWarnings("ConstantConditions")
                    Stream<File> r = Arrays.stream(base.listFiles((_file, name) -> {
                        Matcher matcher;
                        if ((matcher = patternCaseSensitive.matcher(name)).matches()
                                || (matcher = patternCaseInsensitive.matcher(name)).matches())
                        {
                            StringBuilder builder = new StringBuilder(name.length());
                            Iterator<String> groups = IntStream.range(1, matcher.groupCount() + 1).mapToObj(matcher::group).iterator();
                            for (String literalPart : literalParts)
                                builder.append(Optional.ofNullable(literalPart).orElseGet(groups::next));
                            File f = new File(base, builder.toString());
                            return f.exists();
                        }
                        else
                            return false;
                    }));

                    try {
                        if (i + 1 == segments.length) {
                            r = r.flatMap((FunctionThrowsIOException<File, Stream<File>>) (f -> {
                                if (f.isDirectory())
                                    return files(f, i + 1);
                                else
                                    return Stream.of(f);
                            }));
                        }
                        else
                            r = r
                                    .filter(File::isDirectory)
                                    .flatMap((FunctionThrowsIOException<File, Stream<File>>) (dir -> files(dir, i + 1)));
                        }
                    catch (UncheckedIOException e) {
                        throw e.getCause();
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

        @Override
        public String toString() {
            return src + " -> " + base + File.separator + String.join(File.separator, segments);
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
