package dev.liambloom.tests.book.bjp.checker;

public enum TestValidationResult implements ResultVariant {
    Valid(true), Invalid(false), InternalError(false);

    public final boolean isOk;

    TestValidationResult(boolean isOk) {
        this.isOk = isOk;
    }

    @Override
    public boolean isOk() {
        return isOk;
    }
}
