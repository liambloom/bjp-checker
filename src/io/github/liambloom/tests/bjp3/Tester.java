package io.github.liambloom.tests.bjp3;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.net.*;
import java.util.function.*;
import java.lang.reflect.*;

public class Tester {
    private static final Pattern PATH_SEGMENT = Pattern.compile("\\" + File.separatorChar + "[a-zA-Z_\\$][\\w\\$]*");

    private Tester() {}

    public static void main(String[] args) throws MalformedURLException {
        Result<Integer, IllegalArgumentException> r = Result.from(() -> { throw new IllegalStateException(); }, IllegalArgumentException.class);
        System.out.println(r.isOk());
        System.out.println(r.unwrap());
        /*for (Class<?> c : loadClasses()) {
            System.out.println(c.getName());
        }*/
    }

    private static Class<?>[] loadClasses()
        throws MalformedURLException 
    {
        // Get list of files and directories to check
        File here = new File(System.getProperty("user.dir"));
        List<String> files = new LinkedList<>();
        List<URL> dirs = new LinkedList<>();
        getFiles(here, files, dirs);
        String prev = "";
        File parent = here.getParentFile();
        while (parent != null) {
            if (!PATH_SEGMENT.matcher(prev.replace(parent.toString(), "")).matches())
                break;
            dirs.add(parent.toURI().toURL());
            prev = parent.toString();
            parent = parent.getParentFile();
        }

        // Gets class loaders
        URLClassLoader[] loaders = new URLClassLoader[dirs.size()];
        Iterator<URL> urls = dirs.iterator();
        for (int i = 0; urls.hasNext(); i++)
            loaders[i] = new URLClassLoader(new URL[]{urls.next()});

        // Loads classes
        Class<?>[] classes = new Class<?>[files.size()];
        Iterator<String> fileIter = files.iterator();
        for (int i = 0; fileIter.hasNext(); i++) {
            String file = fileIter.next();
            for (URLClassLoader loader : loaders) {
                try {
                    classes[i] = loader.loadClass(file.replace(loader.getURLs()[0].toString(), "").replace("/", "."));
                    break;
                }
                catch (ClassNotFoundException | NoClassDefFoundError e) { assert false; }
            }
            assert classes[i] != null;
        }

        return classes;
    }

    private static void getFiles(final File parent, final List<String> files, final List<URL> dirs)
        throws MalformedURLException 
    {
        int len = files.size();
        for (File f : parent.listFiles()) {
            if (f.isDirectory())
                getFiles(f, files, dirs);
            else if (f.getName().endsWith(".class"))
                files.add(f.toURI().toURL().toString().replace(".class", ""));
        }
        if (len != files.size())
            dirs.add(parent.toURI().toURL());
    }
}

//class SuperResult<T, E extends Throwable> {}

// This is based on rust's core::result::Result
final class Result<T, E extends Throwable>/* extends SuperResult<T, E>*/ {
    @FunctionalInterface
    public static interface ThrowingSupplier<T, E extends Throwable> {
        public T get() throws E;
    }

    private T ok;
    private E error;

    private Result(T ok, E error) {
        this.ok = ok;
        this.error = error;
    }

    public static <T, E extends Throwable> Result<T, E> ok(T ok) {
        return new Result<>(ok, null);
    }

    public static <T, E extends Throwable> Result<T, E> error(E error) {
        if (error == null)
            throw new NullPointerException("You cannot have a null error");
        return new Result<>(null, error);
    }

    public boolean isOk() {
        return error == null;
    }

    public boolean isError() {
        return !isOk();
    }

    public Optional<T> ok() {
        return Optional.ofNullable(ok);
    }

    public Optional<E> error() {
        return Optional.ofNullable(error);
    }

    // TODO: make some sort of convenience method for passing in a Supplier

    public static <T> Result<T, Throwable> from(ThrowingSupplier<T, ? extends Throwable> f) {
        try {
            return Result.ok(f.get());
        }
        catch (Throwable error) {
            return Result.error(error);
        }
        
    }

    public static <T, E extends Throwable> Result<T, E> from(ThrowingSupplier<T, ? extends E> f, Class<E> errorType) {
        try {
            return Result.ok(f.get());
        }
        catch (Throwable error) {
            // The only check exception that f can throw is an exception of type E.
            // "All Throwables except subclasses of java.lang.RuntimeException or
            // java.lang.Error are checked."
            // Therefore, e is of type E, Error or RuntimeException

            if (errorType.isInstance(error))
                return Result.error((E) error);
            else if (error instanceof Error)
                throw (Error) error;
            else
                throw (RuntimeException) error;
        }
    }

    /*public <U> Result<U, E> map(Function<T, U> op) {
        return isOk() ? Result.ok(op.apply(ok)) : Result.error(error);
    }

    public <U> U mapOr(U fallback, Function<T, U> f) {
        return isOk() ? f.apply(ok) : fallback;
    }



    public <U> Result<U, E> and(Result<U, E> res) {
        return this.isError() ? Result.error(error) : res;
    }

    public <U> Result<U, E> andThen(Supplier<Result<U, E>> op) {
        return this.isError() ? Result.error(error) : op.get();
    }*/



    public T unwrap() {
        if (isOk())
            return ok;
        else
            throw new IllegalStateException("Called Result#unwrap() on an `Error` value", error);
    }

    public E unwrapError() {
        if (isError())
            return error;
        else 
            throw new IllegalStateException("Called Result#unwrapError() on an `Ok` value");
    }

    public <S extends E> S unwrapError(Class<S> clazz) {
        if (isError()) {
            if (clazz.isInstance(error))
                return (S) error;
            else
                throw new IllegalStateException("Called Result#unwrapError(Class<S>) on an `Error` value that was not of class S", error);
        }
        else
            throw new IllegalStateException("Called Result#unwrapError(Class<S>) on an `Ok` value");
    }
}
