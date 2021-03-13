package io.github.liambloom.tests.book.bjp3;

import java.io.Closeable;
import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.fusesource.jansi.AnsiConsole;

class Debugger implements Closeable {
    public Debugger() {
        AnsiConsole.systemInstall();
    }

    public void error(String msg, Object... args) {
        System.err.println("\u001b[31m[error]\u001b[0m " + String.format(msg, args));
        //System.exit(1);
    }

    public void internalError(Throwable e) {
        try {
            this.error("Internal Error. Run with `-d' or `--debug' or check logs for more detailed information");
            if (false /* TODO: args.debug */)
                e.printStackTrace();
            System.out.println(Main.class.getResource("/../"));
            final File log = new File(new File(Main.class.getResource("/").toURI()).getParent()
                    + File.separator + "logs" + File.separator
                    + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
            log.getParentFile().mkdir();
            log.createNewFile();
            e.printStackTrace(new PrintStream(log));
        }
        catch (Throwable logError) {
            this.error("Failed to create log file");
            if (false /* TODO: args.debug */)
                logError.printStackTrace();
        }
        finally {
            System.exit(1);
        }
    }

    public void warn(String msg, Object... args) {
        System.err.println("\u001b[33m[warning]\u001b[0m " + String.format(msg, args));
    }

    public void close() {
        AnsiConsole.systemUninstall();
    }
}
