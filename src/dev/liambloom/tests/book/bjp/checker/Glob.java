package dev.liambloom.tests.book.bjp.checker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class Glob {
    public final boolean isTestGlob;
    private final Piece[] pieces;
    private ExecutorService executors = null;
    private int executorUsers;

    public Glob(String[] s) {
        if (s.length == 0)
            throw new UserErrorException("No glob found. For more information, run `check glob --help'")
        pieces = new Piece[s.length];
        isTestGlob = (pieces[0] = new Piece(s[0])).isTestPiece;
        for (int i = 1; i < s.length; i++) {
            pieces[i] = new Piece(s[i]);
            if (pieces[i].isTestPiece != isTestGlob)
                throw new UserErrorException("Cannot validate tests and run checker at same time." + System.lineSeparator()
                        + "For more information, run `check glob --help'");
        }
    }

    private static class Piece {
        final boolean isTestPiece;
        String s;

        public Piece(String s) {
            isTestPiece = s.startsWith("tests/")
                    || File.separatorChar != '/' && s.startsWith("tests" + File.separator)
                    || s.equals("tests");
            this.s = s;
        }

        public Stream<File> files() {

        }
    }

    private synchronized ExecutorService getExecutors() {
        if (executors == null)
             executors = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executorUsers++;
        return executors;
    }

    private synchronized void doneWithExecutors() throws InterruptedException {
        if (--executorUsers == 0) {
            executors.shutdown();
            executors.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }
    }

    public Stream<File> files() throws InterruptedException, ExecutionException {
        //getExecutors();
        //AtomicReference<Throwable> error = new AtomicReference<>();
        Stream<File> r = Arrays.stream(pieces).flatMap(Piece::files)/*
                .map(f -> {
                    try {
                        return f.get();
                    }
                    catch (Throwable e) {
                        try {
                            App.createLogFile(e);
                        } catch (IOException ignored) {}
                        error.compareAndSet(null, e);
                        return null;
                    }
                })*/
                .distinct();
        /*Throwable err = error.get();
        if (err != null) {
            if (err instanceof InterruptedException)
                throw (InterruptedException) err;
            else if (err instanceof ExecutionException)
                throw (ExecutionException) err;
            else if (err instanceof RuntimeException)
                throw (RuntimeException) err;
            else
                throw (Error) err;
        }*/
        //doneWithExecutors();
        return r;
    }
}
