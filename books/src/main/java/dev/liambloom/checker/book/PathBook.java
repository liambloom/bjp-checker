package dev.liambloom.checker.book;

import dev.liambloom.util.function.BiConsumerThrowsException;
import dev.liambloom.util.function.FunctionThrowsException;
import dev.liambloom.util.function.FunctionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class PathBook implements Book {
    private static final Map<Path, List<Consumer<WatchEvent<Path>>>> watcherCallbacks = new HashMap<>();
    private static final Map<FileSystem, WatchService> watchers = new HashMap<>();
    private static final Map<Path, Set<Path>> watcherSymlinkTargets = new HashMap<>();
    private static final ReadWriteLock watcherLock = new ReentrantReadWriteLock();
    private final Map<Consumer<WatchEvent<Path>>, Integer> instanceWatchers = new HashMap<>();
    private Path path;

    PathBook(Path path) {
        this.path = path;
    }

    @Override
    public void addWatcher(Consumer<WatchEvent<Path>> cb) throws IOException {
        watcherLock.writeLock().lock();
        try {
            WatchService watcher = watchers.computeIfAbsent(path.getFileSystem(), FunctionUtils.unchecked((FunctionThrowsException<FileSystem, WatchService>) fileSystem -> {
                WatchService fsWatcher = fileSystem.newWatchService();

                new Thread(() -> {
                    while (true) {
                        WatchKey key;
                        try {
                            key = fsWatcher.take();
                        }
                        catch (InterruptedException e) {
                            System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR, e);
                            break;
                        }

                        watcherLock.readLock().lock();
                        try {
                            for (WatchEvent<?> eventUnfiltered : key.pollEvents()) {
                                if (eventUnfiltered.kind() == OVERFLOW)
                                    continue;

                                @SuppressWarnings("unchecked")
                                WatchEvent<Path> event = (WatchEvent<Path>) eventUnfiltered;
                                Path target = ((Path) key.watchable()).resolve(event.context());
                                Queue<Path> targets = new LinkedList<>();
                                targets.add(target);

                                while (Files.isSymbolicLink(target)) {
                                    Path targetTarget;
                                    try {
                                        targetTarget = Files.readSymbolicLink(target);
                                    } catch (IOException e) {
                                        System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR,
                                            "No longer watching for changes in tests.", e);
                                        return;
                                    }
                                    if (!watcherSymlinkTargets
                                            .computeIfAbsent(targetTarget.toAbsolutePath().normalize(), FunctionUtils.unchecked((FunctionThrowsException<Path, Set<Path>>) e -> {
                                                e.getParent().register(fsWatcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                                                return Collections.synchronizedSet(new HashSet<>());
                                            }))
                                            .add(target.toAbsolutePath().normalize()))
                                        break;
                                    target = targetTarget;
                                }

                                while (!targets.isEmpty()) {
                                    Path currentTarget = targets.remove();

                                    Collection<Consumer<WatchEvent<Path>>> callbacks = watcherCallbacks.get(currentTarget);

                                    if (callbacks == null)
                                        continue;

                                    for (Consumer<WatchEvent<Path>> callback : callbacks){
                                        for (int i = 0; i < instanceWatchers.getOrDefault(callback, 0); i++)
                                            callback.accept(event);
                                    }

                                    Iterator<Path> symlinks = Optional.ofNullable(watcherSymlinkTargets.get(currentTarget))
                                            .map(Collection::iterator)
                                            .orElseGet(Collections::emptyIterator);
                                    while (symlinks.hasNext()) {
                                        Path symlink = symlinks.next();
                                        if (!Files.isSymbolicLink(symlink)) {
                                            symlinks.remove();
                                            continue;
                                        }

                                        Path symlinkTarget;
                                        try {
                                            symlinkTarget = Files.readSymbolicLink(symlink).toAbsolutePath().normalize();
                                        } catch (IOException e) {
                                            System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR,
                                                "No longer watching for changes in tests.", e);
                                            return;
                                        }

                                        if (!symlinkTarget.equals(currentTarget)) {
                                            symlinks.remove();
                                            continue;
                                        }
                                        targets.add(symlinkTarget);
                                    }
                                }
                            }
                        }
                        finally {
                            watcherLock.readLock().unlock();
                        }

                        if (!key.reset()) {
                            System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR,
                                "No longer watching for changes in tests. Reason: WatchKey is invalid");
                            break;
                        }
                    }
                })
                        .start();

                return fsWatcher;
            }));

            watcherCallbacks
                    .computeIfAbsent(path.toAbsolutePath().normalize(),  FunctionUtils.unchecked((FunctionThrowsException<Path, List<Consumer<WatchEvent<Path>>>>) key -> {
                        key.getParent().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                        return Collections.synchronizedList(new LinkedList<>());
                    }))
                    .add(cb);

            while (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                if (!watcherSymlinkTargets
                        .computeIfAbsent(target.toAbsolutePath().normalize(), FunctionUtils.unchecked((FunctionThrowsException<Path, Set<Path>>)key -> {
                            key.getParent().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                            return Collections.synchronizedSet(new HashSet<>());
                        }))
                        .add(path.toAbsolutePath().normalize()))
                    break;
                path = target;
            }

            instanceWatchers.compute(cb, (key, old) -> old == null ? 1 : old + 1);
        }
        finally {
            watcherLock.writeLock().unlock();
        }
    }

    @Override
    public void removeWatcher(Consumer<WatchEvent<Path>> cb) {
        watcherLock.writeLock().lock();
        if (instanceWatchers.computeIfPresent(cb, (key, old) -> old == 1 ? null : old - 1) == null)
            watcherCallbacks.get(path.toAbsolutePath().normalize()).remove(cb);
        watcherLock.writeLock().unlock();
    }

    @Override
    public boolean exists() throws IOException {
        try {
            return path.toRealPath().toString().endsWith(".xml");
        }
        catch (NoSuchFileException e) {
            return false;
        }
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) throws IOException {
        watcherLock.writeLock().lock();
        Map<Consumer<WatchEvent<Path>>, Integer> instanceWatchersCopy = new HashMap<>(instanceWatchers);
        instanceWatchersCopy.forEach((k, v) -> {
            for (int i = 0; i < v; i++)
                removeWatcher(k);
        });
        this.path = path;
        //setPath(getName(), path);
        instanceWatchersCopy.forEach(FunctionUtils.unchecked((BiConsumerThrowsException<Consumer<WatchEvent<Path>>, Integer>) (k, v) -> {
            for (int i = 0; i < v; i++)
                addWatcher(k);
        }));
        watcherLock.writeLock().unlock();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }
    /*protected static void setPath(String name, Path path) throws IOException {
        if (!Files.exists(path) || !path.toRealPath().toString().endsWith(".xml"))
            throw new IllegalArgumentException("Path `" + path + "' is not xml");
        Books.getCustomTests().put(name, path.toString());
    }*/

    @Override
    public Path resolve(Path path) {
        return this.path.resolveSibling(path);
    }

    @Override
    public boolean supportsFileResolution() {
        return true;
    }
}
