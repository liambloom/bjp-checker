package dev.liambloom.tests.bjp.shared;

public enum TestValidationStatus implements Result.Status {
    VALID(Color.GREEN),
    NOT_FOUND(Color.MAGENTA),
    VALID_WITH_WARNINGS(Color.YELLOW),
    INVALID(Color.RED);

    private final Color color;

    TestValidationStatus(Color color) {
        this.color = color;
    }

    @Override
    public Color color() {
        return color;
    }
}