package dev.liambloom.checker.internal;

import dev.liambloom.checker.shared.Color;
import dev.liambloom.checker.shared.Result;

public enum TestStatus implements Result.Status {
    /*
     * The ex/pp has been submitted
     */
//    SUBMITTED(Color.GREEN),

    /*
     * The ex/pp was submitted, and has since been updated. Note that
     * this does not affect the submitted status of the ex/pp.
     */
//    UPDATED(Color.CYAN),

    /**
     * The ex/pp/test passed all tests
     */
    OK(Color.GREEN),


    /**
     * The ex/pp/test could not be found or did not pass all tests
     */
    FAILED(Color.RED),

    /*
     * The ex/pp has not been completed
     */
    //MISSING(Color.GRAY),

    /**
     * Parts of the ex/pp are missing
     */
    INCOMPLETE(Color.GRAY),

    /*
     * The ex/pp was correct at some point, but currently does not pass
     * all tests. This is most likely if there is a single method that
     * is modified in multiple exercises/
     */
//    PREVIOUSLY_DONE(Color.YELLOW),

    /**
     * A method or field corresponding to the ex/pp is not accessible,
     * either because it is not public or because its package is not
     * exported from its respective module.
     */
    BAD_HEADER(Color.RED);

    public final Color color;

    TestStatus(Color color) {
        this.color = color;
    }

    @Override
    public Color color() {
        return color;
    }
}
