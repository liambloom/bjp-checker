package dev.liambloom.tests.book.bjp.checker;

import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;

public class FormattedSystemOutWriter extends FormattedWriter {
    private final Writer systemOut = new OutputStreamWriter(System.out);

    public FormattedSystemOutWriter() {
        AnsiConsole.systemInstall();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        int start = 0;
        for (int i = 0; i < len; i++) {
            char c = cbuf[i + off];
            if (c == '\r') {
                if (i + 1 < len && cbuf[i + off + 1] == '\n')
                    i++;
            }
            else if (c != '\n')
                continue;
            systemOut.write(cbuf, start + off, i + off + 1);
            if (i + 1 != len && indent != 0) {
                char[] indentation = new char[this.indent * 4];
                Arrays.fill(indentation, ' ');
                systemOut.write(indentation);
            }

            start = i + 1;
        }
        systemOut.write(cbuf, start + off, len - start);
    }

    @Override
    public void write(int c) throws IOException {
        systemOut.write(c);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        systemOut.write(str, off, len);
    }

    @Override
    public void setColor(Color color) throws IOException {
        systemOut.write("\u001b[" + color.ansi + "m");
    }

    @Override
    public void flush() throws IOException {
        systemOut.flush();
    }

    @Override
    public void close() throws IOException {
        systemOut.close();
        AnsiConsole.systemUninstall();
    }
}
