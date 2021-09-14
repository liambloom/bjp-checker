package dev.liambloom.checker.bjp.api;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.prefs.Preferences;

public final class App {
    private App() {
    }

    public static final String VERSION = "v1.0.0-alpha-2";

    /**
     * The location of this application's files. The parent folder of "lib" and "bin"
     */
    private static Path here = null;
    private static Preferences prefs = null;
    private static Logger innerLogger = (logKind, msg, args) -> {
        if (logKind == LogKind.ERROR)
            throw new UserErrorException(String.format(msg, args));
    };
    public final static Logger logger = (logKind, msg, args) -> innerLogger.log(logKind, msg, args);

    public static Path here() {
        if (here == null) {
            try {
                Path p = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (Files.isRegularFile(p))
                    p = p.getParent();
                here = p.getParent();
            }
            catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return here;
    }

    public static Preferences prefs() {
        if (prefs == null)
            prefs = Preferences.userRoot().node("dev/liambloom/checker/bjp");
        return prefs;
    }


    public static void setLogger(Logger logger) {
        App.innerLogger = logger;
    }

    public static void createLogFile(Throwable err) throws IOException {
        final Path log = here().resolve("logs").resolve(DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").format(LocalDateTime.now()) + ".log");
        System.out.println(log);
        Files.createDirectories(log.getParent());
        Files.createFile(log);
        err.printStackTrace(new PrintStream(Files.newOutputStream(log)));
        // System.err.println(log); // I can't remember if this was just for debugging
    }
}
