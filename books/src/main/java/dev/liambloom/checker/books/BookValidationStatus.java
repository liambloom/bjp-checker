package dev.liambloom.checker.books;

public enum BookValidationStatus implements Result.Status {
    VALID(Color.GREEN),
    VALID_WITH_WARNINGS(Color.YELLOW),
    NOT_FOUND(Color.MAGENTA),
    INVALID(Color.RED),
    OTHER(Color.GRAY);

    private final Color color;

    BookValidationStatus(Color color) {
        this.color = color;
    }

    @Override
    public Color color() {
        return color;
    }
}