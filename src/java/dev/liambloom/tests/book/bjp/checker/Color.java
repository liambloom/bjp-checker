package dev.liambloom.tests.book.bjp.checker;

public enum Color {
    RED("31"),
    GREEN("32"),
    YELLOW("33"),
    CYAN("36"),
    GRAY("38;5;8");

    public final String ansi;

    Color(String ansi) {
        this.ansi = ansi;
    }
}
