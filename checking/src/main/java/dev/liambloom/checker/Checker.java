package dev.liambloom.checker;

import dev.liambloom.checker.books.*;
import dev.liambloom.checker.internal.CheckerTargetGroup;
import dev.liambloom.checker.internal.Native;
import dev.liambloom.checker.internal.PathClassLoader;
import dev.liambloom.checker.internal.Test;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Checker {
    private final Book book;
    private Set<String> checkableTypeSet = null;
    private Path tempDir = null;


    public Checker(Book book) {
        this.book = book;
    }

    /**
     * Checks according to args
     * @param chapter             The chapter to check, or {@code OptionalInt.empty()} to auto-detect.
     * @param checkables          A map between name for checkables and the checkables of that type to run.
     * @param targets             A stream of classes that should be checked
     * @return A stream containing the results of all checks
     *
     * @throws IOException If an i/o error occurs
     * @throws ClassNotFoundException If the book references a class that could not be found
     * @throws ClassCastException If the book's chapter or checkable annotations are not actually annotations, or if their
     *              {@code value()} methods don't return {@code int}s.
     * @throws NoSuchMethodException If the book's chapter or checkable annotations don't have a {@code value()} method
     * @throws IllegalAccessException If the book's chapter or checkable annotations' {@code value()} method is not accessible
     * @throws InvocationTargetException If the book's chapter or checkable annotations' {@code value()} method throws an exception
     */
    @SuppressWarnings("RedundantThrows")
    public Result<TestStatus>[] check(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalInt chapter, Map<String, boolean[]> checkables,
                                      Stream<Path> targets) throws IOException, ClassNotFoundException, NoSuchMethodException,
        IllegalAccessException, InvocationTargetException, URISyntaxException {
        for (String checkable : checkables.keySet()) {
            if (!getCheckableTypeSet().contains(checkable))
                throw new IllegalArgumentException("Checkable \"" + checkable + "\" does not exist");
        }

        int checkedChapter;
//        logger.log(System.Logger.Level.DEBUG, "Target annotation types: %s", );s
        AtomicInteger detectedChapter = new AtomicInteger(-1);
        List<Class<?>> classes = new PathClassLoader(targets, book.getMeta().classLoader()).loadAllOwnClasses()
//            .peek(System.out::println)
            .filter(clazz -> {
//                    System.Logger logger = System.getLogger(Util.generateLoggerName());
//                    logger.log(System.Logger.Level.TRACE, "%s has annotations %s", clazz, Arrays.toString(clazz.getAnnotations()));
                    return Arrays.stream(clazz.getAnnotationsByType(book.getMeta().chapterType().annotation()))
                        .map(FunctionUtils.unchecked((FunctionThrowsException<Annotation, Integer>)
                            a -> book.getMeta().chapterType().valueGenetic(a)))
                        .anyMatch(c -> {
                            if (chapter.isPresent())
                                return c == chapter.getAsInt();
                            else if (detectedChapter.compareAndExchange(-1, c) != c)
                                throw new IllegalArgumentException("Cannot auto detect chapter, as classes belonging to chapters "
                                    + c + " and " + detectedChapter + " were found");
                            else
                                return true;
                        });
                }
            )
            .toList();
        checkedChapter = chapter.orElseGet(detectedChapter::get);

        Targets potentialTargets = new Targets();
        Queue<Class<?>> classQ = new LinkedList<>(classes);
        while (!classQ.isEmpty()) {
            Class<?> c = classQ.remove();
            potentialTargets.classes().add(c);
            Set<Class<?>> inner = new HashSet<>();
            Collections.addAll(inner, c.getClasses());
            Collections.addAll(inner, c.getDeclaredClasses());
            classQ.addAll(inner);
        }
        potentialTargets.classes()
            .stream()
            .flatMap(c -> Stream.of(c.getFields(), c.getDeclaredFields(), c.getMethods(), c.getDeclaredMethods(), c.getDeclaredConstructors()))
            .flatMap(Stream::of)
            .forEach(potentialTargets::add);

        if (tempDir == null)
            tempDir = Files.createTempDirectory(null);


        String base = book.getMeta().resourceBase().normalize().getPath();
        if (!base.endsWith("/"))
            base += "/";
        URI bookUri = book.getLocator().url().toURI().normalize();
        URI absoluteBase = bookUri.resolve(base).normalize();

        for (URI uri : book.getMeta().resources()) {
            URI resolved = book.getLocator().url().toURI().resolve(uri).normalize();

            Path p = tempDir.resolve(absoluteBase.relativize(resolved).getPath());
            Files.createDirectories(p.getParent());
            Files.copy(resolved.toURL().openStream(), p);
        }

        Path working = tempDir.resolve(absoluteBase.relativize(bookUri).getPath()).getParent();
        Files.createDirectories(working);
        Native.changeDirectory(working);

        InputStream prevIn = System.in;
        PrintStream prevOut = System.out;
        PrintStream prevErr = System.err;
        try {
            List<Future<Result<TestStatus>>> futures = Arrays.stream(book.getMeta().checkableTypes())
                .parallel()
                .map(a -> new CheckerTargetGroup<>(a, checkables.getOrDefault(a.name(), new boolean[0])))
                .flatMap(FunctionUtils.unchecked((FunctionThrowsException<CheckerTargetGroup<?>, Stream<Test>>) (e -> {
                    for (AnnotatedElement potential : potentialTargets)
                        e.addPotentialTarget(potential);
                    return e.apply(book.getChapter(checkedChapter));
                })))
                .map(Test::start)
                .collect(Collectors.toList());
            Result<TestStatus>[] r = new Result[futures.size()];
            Iterator<Future<Result<TestStatus>>> iter = futures.iterator();
            for (int j = 0; iter.hasNext(); j++)
                r[j] = iter.next().get();
            return r;
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        finally {
            System.setIn(prevIn);
            System.setOut(prevOut);
            System.setErr(prevErr);
            Native.resetDir();
        }
    }

    private void removeTempDir() throws IOException {
        if (tempDir == null)
            return;
        Files.walk(tempDir)
            .sorted(Comparator.comparing(Path::getNameCount))
            .forEach(FunctionUtils.unchecked(Files::delete));
    }

    private synchronized Set<String> getCheckableTypeSet() {
        if (checkableTypeSet == null)
            checkableTypeSet = Arrays.stream(book.getMeta().checkableTypes()).map(CheckableType::name).collect(Collectors.toSet());
        return checkableTypeSet;
    }
}
