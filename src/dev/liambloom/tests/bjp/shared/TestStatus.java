package dev.liambloom.tests.bjp.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public enum TestStatus implements Result.Status {
    /**
     * The ex/pp has been submitted
     */
    SUBMITTED(Color.GREEN),

    /**
     * The ex/pp was submitted, and has since been updated. Note that
     * this does not affect the submitted status of the ex/pp.
     */
    UPDATED(Color.CYAN),

    /**
     * The ex/pp/test passed all tests
     */
    CORRECT(Color.GREEN),

    /**
     * The ex/pp has not been completed
     */
    MISSING(Color.GRAY),

    /**
     * The ex/pp was correct at some point, but currently does not pass
     * all tests. This is most likely if there is a single method that
     * is modified in multiple exercises/
     */
    PREVIOUSLY_DONE(Color.YELLOW),

    /**
     * A method or field corresponding to the ex/pp is not accessible,
     * either because it is not public or because its package is not
     * exported from its respective module.
     */
    METHOD_INCACCESABLE(Color.RED),

    /**
     * The ex/pp/test could not be found or did not pass all tests
     */
    INCORRECT(Color.RED);

    public final Color color;

    TestStatus(Color color) {
        this.color = color;
    }

    @Override
    public Color color() {
        return color;
    }
}
