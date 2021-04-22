package dev.liambloom.tests.book.bjp.checker;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

public class Glob {
    public final boolean isTestGlob;
    private final Piece[] pieces;

    public Glob(String[] s) {
        if (s.length == 0)
            throw new UserErrorException("No glob found. For more information, run `check glob --help'")
        pieces = new Piece[s.length];
        isTestGlob = (pieces[0] = new Piece(s[0])).isTestPiece;
        for (int i = 1; i < s.length; i++) {
            pieces[i] = new Piece(s[i]);
            if (pieces[i].isTestPiece != isTestGlob)
                throw new UserErrorException("Cannot validate tests and run checker at same time." + System.lineSeparator()
                        + "For more information, run `check glob --help'");
        }
    }

    private static class Piece {
        final boolean isTestPiece;
        String s;

        public Piece(String s) {
            isTestPiece = s.startsWith("tests/") || s.equals("tests");
            this.s = s;
        }

        public Stream<File> files() {

        }
    }

    public Stream<File> files() {
        return Arrays.stream(pieces)
                .parallel()
                .unordered()
                .flatMap(Piece::files)
                .distinct()
                .sorted();
    }
}
