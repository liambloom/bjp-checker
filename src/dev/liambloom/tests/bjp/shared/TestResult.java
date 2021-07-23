package dev.liambloom.tests.bjp.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class TestResult extends Result {
    public final Checked.Type type;
    public final int num;
    public final Throwable error;
    public final ByteArrayOutputStream out;

    public TestResult(Checked.Type type, int num, Variant variant) {
        this(type, num, variant, null, null);
    }

    // TODO: This doesn't work with incorrect prints, only with thrown errors
    public TestResult(Checked.Type type, int num, Variant variant, Throwable error, ByteArrayOutputStream out) {
        super(type + " " + num, variant);
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
        SUBMITTED(true, Color.GREEN),

        /**
         * The ex/pp was submitted, and has since been updated. Note that
         * this does not affect the submitted status of the ex/pp.
         */
        UPDATED(true, Color.CYAN),

        /**
         * The ex/pp/test passed all tests
         */
        CORRECT(true, Color.GREEN),

        /**
         * The ex/pp has not been completed
         */
        NOT_DONE(false, Color.GRAY),

        /**
         * The ex/pp was correct at some point, but currently does not pass
         * all tests. This is most likely if there is a single method that
         * is modified in multiple exercises/
         */
        PREVIOUSLY_DONE(false, Color.YELLOW),

        /**
         * A method or field corresponding to the ex/pp is not accessible,
         * either because it is not public or because its package is not
         * exported from its respective module.
         */
        METHOD_INCACCESABLE(false, Color.RED),

        /**
         * The ex/pp/test did not pass all tests
         */
        INCORRECT(false, Color.RED);

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
