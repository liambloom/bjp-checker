package dev.liambloom.tests.bjp.shared;

import java.io.IOException;
import java.io.OutputStream;

public abstract class Result {
    public final String name;
    public final Variant variant;

    public Result(String name, Variant variant) {
        this.name = name;
        this.variant = variant;
    }

    public abstract void printToStream(OutputStream stream) throws IOException;

    public interface Variant {
        boolean isOk();

        default boolean isError() {
            return !isOk();
        }

        default Color color() {
            return isOk() ? Color.GREEN : Color.RED;
        }

        default String getName() {
            return toString().replace("_", " ").toLowerCase();
        }
    }
}
