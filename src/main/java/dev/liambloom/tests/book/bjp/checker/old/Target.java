package dev.liambloom.tests.book.bjp.checker.old;

public class Target {
    // Compare flags using identity equality (==).
    public static final int MIN_CH_NUM = 1;
    public static final int AUTO_CH_FLAG = MIN_CH_NUM - 1;

    public static final int MIN_EX_NUM = 1;
    public static final int MAX_EX_NUM = 40;
    public static final boolean[] ALL_EX_FLAG = new boolean[0];
    public static final boolean[] MISSING_EX_FLAG = new boolean[0]; // tests all exercises not currently marked as "correct"

    public static final int MIN_PP_NUM = 1;
    public static final int MAX_PP_NUM = 10;
    public static final boolean[] ALL_PP_FLAG = new boolean[0];
    public static final boolean[] MISSING_PP_FLAG = new boolean[0]; // tests all exercises not currently marked as "correct"

    public final int chapter;
    public final boolean[] exercise;
    public final boolean[] programmingProject;
    public final String[] glob;

    public Target(int chapter, boolean[] exercise, boolean[] programmingProject, String[] glob) {
        this.chapter = chapter;
        this.exercise = exercise;
        this.programmingProject = programmingProject;
        this.glob = glob;
    }


}
