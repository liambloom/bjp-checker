package dev.liambloom.tests.book.bjp.checker;

public enum TestResult {
    /**
     * The ex/pp has been submitted
     */
    Submitted,

    /**
     * The ex/pp was submitted, and has since been updated. Note that
     * this does not affect the submitted status of the ex/pp.
     */
    Updated,

    /**
     * The ex/pp/test passed all tests
     */
    Correct,

    /**
     * The ex/pp has not been completed
     */
    NotDone,

    /**
     * The ex/pp was correct at some point, but currently does not pass
     * all tests. This is most likely if there is a single method that
     * is modified in multiple exercises/
     */
    PreviouslyDone,

    /**
     * The ex/pp/test did not pass all tests
     */
    Incorrect
}
