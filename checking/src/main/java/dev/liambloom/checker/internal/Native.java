package dev.liambloom.checker.internal;

import dev.liambloom.checker.Checker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class Native {
    public static final Path initDir = Path.of(".").toAbsolutePath().normalize();
    private static final AtomicBoolean loadAttempt = new AtomicBoolean(false);
    private static final Thread dirResetter = new Thread(Native::resetDir);
    private static boolean loadSuccess = false;

    public static void resetDir() {
        changeDirectory(initDir);
    }

    /**
     * Changes the current working directory
     *
     * @param path The directory to change to
     * @return returns true if the native library was successfully loaded
     */
    public static boolean changeDirectory(Path path) {
        loadAttempt.compareAndSet(false, load());
        if (loadSuccess) {
            if (!initDir.equals(path))
                Runtime.getRuntime().addShutdownHook(dirResetter);
            changeDirectory(path.toString());
            if (initDir.equals(path))
                Runtime.getRuntime().removeShutdownHook(dirResetter);
        }
        return loadSuccess;
    }

    /**
     * Loads the library, sets {@link #loadSuccess} to reflect the
     * success or failure of the operation.
     *
     * @return {@code true}
     */
    private static boolean load() {
        Path tempDir = null;
        Path tempFile = null;
        try {
            tempDir = Files.createTempDirectory(null);
            tempFile = tempDir.resolve(System.mapLibraryName("native"));
            Files.copy(Objects.requireNonNull(Native.class.getResourceAsStream("/native/"
                + System.getProperty("os.arch") + "/"
                + System.getProperty("os.name") + "/"
                + System.mapLibraryName("native"))), tempFile);
            System.load(tempFile.toAbsolutePath().toString());
            loadSuccess = true;
        }
        catch (UnsatisfiedLinkError | IOException | NullPointerException e) {
            loadSuccess = false;
            System.getLogger(Native.class.getName()).log(System.Logger.Level.WARNING,
                "Failed to load native library for " + System.getProperty("os.name")
                    + " on " + System.getProperty("os.arch"),
                e);
        }
        finally {
            try {
                if (tempFile != null)
                    Files.deleteIfExists(tempFile);
                if (tempDir != null)
                    Files.deleteIfExists(tempDir);
            }
            catch (IOException e) {
                System.getLogger(Native.class.getName()).log(System.Logger.Level.DEBUG, "Error deleting temp file for native library", e);
            }
        }
        return true;
    }

    public static native void changeDirectory(String path);
}
