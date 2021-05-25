package dev.liambloom.tests.book.bjp.checker;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SecureGlobClassLoader extends ClassLoader {
    static final int JAVA_VERSION;

    static {
        String vStr = System.getProperty("java.version");
        int vDot = vStr.indexOf('.');
        // v = The version number, or 1 if the version is <=8
        JAVA_VERSION = Integer.parseInt(vDot == -1 ? vStr : vStr.substring(0, vDot));
    }

    private final SortedMap<String, byte[]> classBytes;
    private final List<Class<?>> loadedClasses = new ArrayList<>();
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

    public SecureGlobClassLoader(Glob glob) throws IOException {
        this(glob, getSystemClassLoader());
    }

    public SecureGlobClassLoader(Glob glob, ClassLoader parent) throws IOException {
        super(parent);
        try {
            classBytes = glob.files()
                    .map(File::toPath)
                    .map((FunctionThrowsIOException<Path, Path>) Glob::readSymbolicLink)
                    .flatMap((FunctionThrowsIOException<Path, Stream<ClassSource>>) (p -> {
                        if (p.toString().endsWith(".jar")) {
                            // TODO: Test if this works with MRJARs (it should, but it's untested)
                            JarFile jar = new JarFile(p.toFile());
                            Stream<ClassSource> classFiles;
                            String vStr = System.getProperty("java.version");
                            int vDot = vStr.indexOf('.');
                            // v = The version number, or 1 if the version is <=8
                            int v = Integer.parseInt(vDot == -1 ? vStr : vStr.substring(0, vDot));
                            if (v > 8 && Optional.ofNullable(jar.getManifest().getMainAttributes().getValue("Multi-Release"))
                                    .map(String::toLowerCase)
                                    .map("true"::equals)
                                    .orElse(false))
                            {
                                Map<String, ClassSource> entryMap = new HashMap<>();
                                Enumeration<JarEntry> entries = jar.entries();
                                while (entries.hasMoreElements()) {
                                    JarEntry entry = entries.nextElement();
                                    String name = entry.getName();
                                    if (entry.isDirectory() || !name.toLowerCase().endsWith(".class"))
                                        continue;
                                    if (name.substring(0, 9).equalsIgnoreCase("META-INF/")) {
                                        if (name.substring(9, 17).equalsIgnoreCase("version/")) {
                                            int ev = Integer.parseInt(name.substring(17, name.indexOf('/', 17)));
                                            if (ev <= v)
                                                entryMap.compute(name.substring(name.indexOf('/', 17)), (BiFunctionThrowsIOException<String, ClassSource, ClassSource>)
                                                        (k, val) -> val == null || val.version() < ev ? new ClassSource(k.substring(0, k.length() - 6).replace('/', '.'), jar.getInputStream(entry), ev) : val);
                                        }
                                    }
                                    else
                                        entryMap.put(name, new ClassSource(name.substring(0, name.length() - 6).replace('/', '.'), jar.getInputStream(entry), 8));
                                }
                                classFiles = entryMap.values().stream();
                            }
                            else {
                                classFiles = jar.stream()
                                        .filter(e -> !e.isDirectory() && e.getName().toLowerCase().endsWith(".class") && !e.getName().toUpperCase().startsWith("META-INF"))
                                        .map((FunctionThrowsIOException<JarEntry, ClassSource>)
                                                (e -> new ClassSource(e.getName().substring(0, e.getName().length() - 6).replace('/', '.'), jar.getInputStream(e), 8)));
                            }

                            return classFiles;
                        }
                        else if (p.toString().endsWith(".class")){
                            String abs = p.toAbsolutePath().toString();
                            return Stream.of(new ClassSource(abs.substring(0, abs.length() - 6).replace(File.separatorChar, '.'), new FileInputStream(p.toFile())));
                        }
                        else
                            // Somewhere, somehow, this gets wrapped in another UserErrorException (I think).
                            // Nevermind, it's fine now
                            throw new UserErrorException("Unable to load classes from `" + p + "' because it is not a .class or .jar file");
                    }))
            .collect(Collectors.toMap(
                    s -> new StringBuilder(s.name()).reverse().toString(),
                    (FunctionThrowsIOException<ClassSource, byte[]>) ClassSource::bytes,
                    (v1, v2) -> { throw new IllegalStateException("Duplicate key in classBytes"); },
                    () -> Collections.synchronizedSortedMap(new TreeMap<>()) /* TreeMap<String, byte[]>::new */));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public Class<?>[] loadAll() {
        Lock lock = rwlock.writeLock();
        while (!classBytes.isEmpty()) {
            System.out.println("Locking write lock");
            lock.lock();
            System.out.println("Locked write lock");
            Iterator<byte[]> iter = classBytes.values().iterator();
            byte[] b = iter.next();
            iter.remove();
            System.out.println("Unlocking write lock");
            lock.unlock();
            System.out.println("Unlocked write lock");
            defineClass(null, b, 0, b.length);
        }
        return loadedClasses.toArray(new Class<?>[0]);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        System.out.println(name);
        Lock lock = rwlock.readLock();
        StringBuilder nameMut = new StringBuilder(name).reverse();
        String from = nameMut.toString();
        nameMut.setCharAt(name.length() - 1, (char) (nameMut.charAt(name.length() - 1) + 1));
        String to = nameMut.toString();
        System.out.println("Locking read lock");
        lock.lock();
        System.out.println("Locked read lock");
        Iterator<byte[]> iter = classBytes.subMap(from, to).values().iterator();
        while (iter.hasNext()) {
            byte[] b = iter.next();
            Class<?> clazz;
            try {
                clazz = defineClass(name, b, 0, b.length);
            }
            catch (NoClassDefFoundError ignored) {
                continue;
            }
            loadedClasses.add(clazz);
            iter.remove();
            System.out.println("Unlocking read lock");
            lock.unlock();
            System.out.println("Unlocked read lock");
            return clazz;
        }
        System.out.println("Unlocking read lock");
        lock.unlock();
        System.out.println("Unlocked read lock");
        return super.findClass(name);
    }
}

class ClassSource {
    private final int version;
    private InputStream stream;
    private final String name;

    // TODO: Add better constructors

    /*public ClassSource(File f) throws FileNotFoundException {
        this(f.getAbsolutePath(), new FileInputStream(f));
    }*/

    public ClassSource(String name, InputStream stream) {
        this(name, stream, 8);
    }

    public ClassSource(String name, InputStream stream, int version) {
        /*if (name.endsWith(".class"))
            name = name.substring(0, name.length() - 6);*/
        this.name = name;
        this.stream = stream;
        this.version = version;
    }

    public int version() {
        return version;
    }

    public String name() {
        return name;
    }

    public byte[] bytes() throws IOException {
        ByteArrayOutputStream bufs = new ByteArrayOutputStream();
        byte[] buf = new byte[0x400]; // 1kb
        int len;
        while ((len = stream.read(buf)) != -1)
            bufs.write(buf, 0, len);
        stream = null;
        return bufs.toByteArray();
    }
}

