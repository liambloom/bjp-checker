package dev.liambloom.checker.bjp.cli;

import dev.liambloom.checker.bjp.api.App;
import dev.liambloom.checker.bjp.api.FunctionThrowsIOException;
import dev.liambloom.checker.bjp.api.LogKind;
import dev.liambloom.checker.bjp.api.UserErrorException;

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

    public Glob(Collection<String> s) throws IOException {
        if (s.size() == 0)
            throw new UserErrorException("No glob found. For more information, run `check glob --help'");
        Iterator<String> iter = s.iterator();
        pieces = new Piece[s.size()];
        for (int i = 0; iter.hasNext(); i++)
            pieces[i] = new Piece(iter.next());
    }

    public Glob(String... s) throws IOException {
        this(Arrays.asList(s));
    }

    private static class Piece {
        final Path base;
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
            if (s.startsWith("/"))
                base = Path.of(File.separator).toRealPath();
            else
                base = Path.of(".").toRealPath();
            segments = segmentsList.toArray(new String[0]);
        }

        public List<Path> files() throws IOException {
            List<Path> r = files(base, 0)
                .collect(Collectors.toList());
            if (r.size() == 0) {
                if (raw.contains(File.separator) && !raw.contains("/")) {
                    r = new Piece(raw.replace(File.separatorChar, '/'), src).files();
                    //App.logger.log(LogKind.WARN, "Using \"\\\" instead of \"/\" is slower and does not support escaping");
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
                App.logger.log(LogKind.WARN, "\"**\" as part of a larger segment is interpreted as two single stars");
        }

        private Stream<Path> files(Path base, int i) throws IOException {
            assert Files.isDirectory(base);
            if (segments.length == i)
                return Files.walk(base, FileVisitOption.FOLLOW_LINKS)
                    .filter(Files::isRegularFile);
            final String segment = segments[i];
            switch (segment) {
                case ".":
                    return files(base, i + 1);
                case "..":
                    return files(base.getParent(), i + 1);
                case "**":
                    if (segments.length > i + 1) {
                        try {
                            return Stream.concat(
                                files(base, i + 1),
                                Files.list(base)
                                    .map((FunctionThrowsIOException<Path, Path>) Path::toRealPath)
                                    .filter(Files::isDirectory)
                                    .flatMap((FunctionThrowsIOException<Path, Stream<Path>>) (dir -> files(dir, i)))
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
                    final Pattern patternCaseSensitive = Pattern.compile(patternBuilder.toString());
                    final Pattern patternCaseInsensitive = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
                    //@SuppressWarnings("ConstantConditions")
                    Stream<Path> r = Files.list(base)
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            Matcher matcher;
                            if ((matcher = patternCaseSensitive.matcher(name)).matches()
                                || (matcher = patternCaseInsensitive.matcher(name)).matches()) {
                                StringBuilder builder = new StringBuilder(name.length());
                                Iterator<String> groups = IntStream.range(1, matcher.groupCount() + 1).mapToObj(matcher::group).iterator();
                                for (String literalPart : literalParts)
                                    builder.append(Optional.ofNullable(literalPart).orElseGet(groups::next));
                                return Files.exists(base.resolve(builder.toString()));
                            }
                            else
                                return false;
                        });

                    try {
                        if (i + 1 == segments.length) {
                            r = r.flatMap((FunctionThrowsIOException<Path, Stream<Path>>) (p -> {
                                if (Files.isDirectory(p))
                                    return files(p, i + 1);
                                else
                                    return Stream.of(p);
                            }));
                        }
                        else
                            r = r
                                .filter(Files::isDirectory)
                                .flatMap((FunctionThrowsIOException<Path, Stream<Path>>) (dir -> files(dir, i + 1)));
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

        /*@Override
        public String toString() {
            return src + " -> " + base + File.separator + String.join(File.separator, segments);
        }*/
    }

    public Stream<Path> files() throws IOException {
        try {
            return Arrays.stream(pieces)
                .unordered()
                .parallel()
                .map((FunctionThrowsIOException<Piece, List<Path>>) Piece::files)
                .flatMap(List::stream)
                .distinct()
                .sorted();
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public Path single() throws IOException {
        return files().collect(Collectors.collectingAndThen(Collectors.toList(),
            list -> {
                if (list.size() != 1)
                    throw new UserErrorException("Expected single file, but glob matched " + list.size());
                return list.get(0);
            }
        ));
    }
}
