package dev.liambloom.checker;

import dev.liambloom.checker.Color;
import dev.liambloom.checker.Result;

public enum TestValidationStatus implements Result.Status {
    VALID(Color.GREEN),
    VALID_WITH_WARNINGS(Color.YELLOW),
    NOT_FOUND(Color.MAGENTA),
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