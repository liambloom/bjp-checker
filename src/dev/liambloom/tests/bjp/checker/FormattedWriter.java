package dev.liambloom.tests.bjp.checker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;

public abstract class FormattedWriter extends Writer {
    protected int indent = 0;

    public abstract void setColor(Color color) throws IOException;
    public void setIndent(int indent) {
        this.indent = indent;
    }
}
