package dev.liambloom.tests.bjp.shared;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class PathBook extends ModifiableBook {
    private static final Map<Path, List<Consumer<WatchEvent<Path>>>> watcherCallbacks = new HashMap<>();
    private static final Map<FileSystem, WatchService> watchers = new HashMap<>();
    private static final Map<Path, Set<Path>> watcherSymlinkTargets = new HashMap<>();
    private static final ReadWriteLock watcherLock = new ReentrantReadWriteLock();
    private final Map<Consumer<WatchEvent<Path>>, Integer> instanceWatchers = new HashMap<>();
    private Path path;

    PathBook(String name, Path path) {
        super(name);
        this.path = path;
    }

    @Override
    public Document getDocument(DocumentBuilder db) throws SAXException, IOException {
        if (exists())
            return db.parse(Files.newInputStream(path));
        else
            throw new NoSuchFileException(path.toString());
    }

    @Override
    public void addWatcher(Consumer<WatchEvent<Path>> cb) throws IOException {
        watcherLock.writeLock().lock();
        try {
            WatchService watcher = watchers.computeIfAbsent(path.getFileSystem(), (FunctionThrowsIOException<FileSystem, WatchService>) fileSystem -> {
                WatchService fsWatcher = fileSystem.newWatchService();

                new Thread(() -> {
                    while (true) {
                        WatchKey key;
                        try {
                            key = fsWatcher.take();
                        }
                        catch (InterruptedException e) {
                            App.logger.log(LogKind.ERROR, e.toString());
                            throw new RuntimeException(e);
                        }

                        watcherLock.readLock().lock();
                        try {
                            for (WatchEvent<?> eventUnfiltered : key.pollEvents()) {
                                if (eventUnfiltered.kind() == OVERFLOW)
                                    continue;

                                @SuppressWarnings("unchecked")
                                WatchEvent<Path> event = (WatchEvent<Path>) eventUnfiltered;
                                Path target = ((Path) key.watchable()).resolve(event.context());//.toAbsolutePath().normalize();
                                Queue<Path> targets = new LinkedList<>();
                                targets.add(target);

                                while (Files.isSymbolicLink(target)) {
                                    Path targetTarget;
                                    try {
                                        targetTarget = Files.readSymbolicLink(target);
                                    } catch (IOException e) {
                                        App.logger.log(LogKind.ERROR, "No longer watching for changes in tests. Reason: " + e.getMessage());
                                        return;
                                    }
                                    if (!watcherSymlinkTargets
                                            .computeIfAbsent(targetTarget.toAbsolutePath().normalize(), (FunctionThrowsIOException<Path, Set<Path>>) (e -> {
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

                                    for (Consumer<WatchEvent<Path>> callback : callbacks)
                                        callback.accept(event);

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
                                            App.logger.log(LogKind.ERROR, "No longer watching for changes in tests. Reason: " + e.getMessage());
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
                            App.logger.log(LogKind.ERROR, "No longer watching for changes in tests. Reason: WatchKey is invalid");
                            break;
                        }
                    }
                })
                        .start();

                return fsWatcher;
            });

            watcherCallbacks
                    .computeIfAbsent(path.toAbsolutePath().normalize(), (FunctionThrowsIOException<Path, List<Consumer<WatchEvent<Path>>>>) (key -> {
                        key.getParent().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                        return Collections.synchronizedList(new LinkedList<>());
                    }))
                    .add(cb);

            while (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                if (!watcherSymlinkTargets
                        .computeIfAbsent(target.toAbsolutePath().normalize(), (FunctionThrowsIOException<Path, Set<Path>>) (key -> {
                            key.getParent().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                            return Collections.synchronizedSet(new HashSet<>());
                        }))
                        .add(path.toAbsolutePath().normalize()))
                    break;
                path = target;
            }

            instanceWatchers.compute(cb, (key, old) -> Optional.ofNullable(old).map(i -> i + 1).orElse(1));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
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
            return path.toRealPath().toString().endsWith(".xml") && super.exists();
        }
        catch (NoSuchFileException e) {
            return false;
        }
    }

    @Override
    public String getName() {
        return name;
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
        setPath(getName(), path);
        instanceWatchersCopy.forEach((BiConsumerThrowsIOException<Consumer<WatchEvent<Path>>, Integer>) ((k, v) -> {
            for (int i = 0; i < v; i++)
                addWatcher(k);
        }));
        watcherLock.writeLock().unlock();
    }

    protected static void setPath(String name, Path path) throws IOException {
        if (!Files.exists(path) || !path.toRealPath().toString().endsWith(".xml"))
            throw new UserErrorException("Path `" + path + "' is not xml");
        Book.getCustomTests().put(name, path.toString());
    }

    @Override
    protected Source getSource() throws IOException {
        return new StreamSource(new BufferedInputStream(Files.newInputStream(path)));
    }
}
