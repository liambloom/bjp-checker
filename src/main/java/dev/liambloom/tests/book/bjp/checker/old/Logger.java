package dev.liambloom.tests.book.bjp.checker.old;

import java.io.File;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

interface Logger {
    void notice(String msg, Object... args);
    void warn(String msg, Object... args);
    void error(String msg, Object... args);
    default void internalError(Throwable err) {
        try {
            error("Internal Error. Check logs for more detailed information");
            /* if (debugMode)
                err.printStackTrace(); */
            /* System.out.println(App.here()
                    + File.separator + "logs" + File.separator
                    + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log"); */
            final File log = new File(new File(App.here()).getParent()
                    + File.separator + "logs" + File.separator
                    + DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
            log.getParentFile().mkdir();
            log.createNewFile();
            err.printStackTrace(new PrintStream(log));
        }
        catch (Throwable logError) {
            error("Failed to create log file");
            /*if (debugMode)
                logError.printStackTrace();*/
        }
    }
}
