package dev.liambloom.tests.bjp.checker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Pattern;

public abstract class Result {
    public final String name;
    public final Variant variant;

    public Result(String name, Variant variant) {
        this.name = name;
        this.variant = variant;
    }

    abstract void printToStream(OutputStream stream) throws IOException;

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
