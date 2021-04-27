package dev.liambloom.tests.book.bjp.checker;

import static org.fusesource.jansi.Ansi.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
        Pattern SPACES_EXCEPT_FIRST = Pattern.compile("(?<!^)[A-Z]");

        boolean isOk();

        default boolean isError() {
            return !isOk();
        }

        default Color color() {
            return isOk() ? Color.GREEN : Color.RED;
        }

        default String getName() {
            return SPACES_EXCEPT_FIRST.matcher(toString()).replaceAll(" $1").toLowerCase();
        }
    }
}
