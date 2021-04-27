package dev.liambloom.tests.book.bjp.checker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.fusesource.jansi.Ansi.Color;

public class TestResult extends Result {
    public static final boolean EX = false;
    public static final boolean PP = true;

    public final boolean type;
    public final int num;
    public final Throwable error;
    public final ByteArrayOutputStream out;

    public TestResult(boolean type, int num, Variant variant) {
        this(type, num, variant, null, null);
    }

    public TestResult(boolean type, int num, Variant variant, Throwable error, ByteArrayOutputStream out) {
        super((type == EX ? "Exercise" : "Programming Project") + " " + num, variant);
        this.type = type;
        this.num = num;
        this.error = error;
        this.out = out;

        if (variant.isOk == (error == null))
            throw new IllegalArgumentException("An error should be provided with error variants, but not ok variants");
    }

    @Override
    public void printToStream(OutputStream stream) throws IOException {
        if (variant.isOk())
            throw new IllegalStateException("Result#printToStream should only be called on error values");
        error.printStackTrace(new PrintStream(stream));
        out.writeTo(stream);
    }

    public enum Variant implements Result.Variant {
        /**
         * The ex/pp has been submitted
         */
        Submitted(true, Color.GREEN),

        /**
         * The ex/pp was submitted, and has since been updated. Note that
         * this does not affect the submitted status of the ex/pp.
         */
        Updated(true, Color.BLUE),

        /**
         * The ex/pp/test passed all tests
         */
        Correct(true, Color.GREEN),

        /**
         * The ex/pp has not been completed
         */
        NotDone(false, Color.YELLOW),

        /**
         * The ex/pp was correct at some point, but currently does not pass
         * all tests. This is most likely if there is a single method that
         * is modified in multiple exercises/
         */
        PreviouslyDone(false, Color.YELLOW),

        /**
         * The ex/pp/test did not pass all tests
         */
        Incorrect(false, Color.RED);

        public final boolean isOk;
        public final Color color;

        Variant(boolean isOk, Color color) {
            this.isOk = isOk;
            this.color = color;
        }

        @Override
        public boolean isOk() {
            return isOk;
        }

        @Override
        public Color color() {
            return color;
        }
    }
}
