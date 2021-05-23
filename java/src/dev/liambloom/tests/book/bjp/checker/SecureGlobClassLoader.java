package dev.liambloom.tests.book.bjp.checker;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SecureGlobClassLoader extends ClassLoader {
    private final Lock lock = new ReentrantLock();
    private final Map<String, Condition> loadConditions = Collections.synchronizedMap(new HashMap<>());
    private boolean isLoading = false;
    private final Glob glob;

    public SecureGlobClassLoader(Glob glob) {
        this(glob, getSystemClassLoader());
    }

    public SecureGlobClassLoader(Glob glob, ClassLoader parent) {
        super(parent);
        this.glob = glob;
    }

    public Class<?>[] loadAll() throws IOException {
        Queue<byte[]> q1;
        try {
             q1 = glob.files()
                    .map(File::toPath)
                    .map((FunctionThrowsIOException<Path, Path>) Glob::readSymbolicLink)
                    .flatMap((FunctionThrowsIOException<Path, Stream<byte[]>>) (p -> {
                        if (p.toString().endsWith(".jar")) {
                            // TODO: Test if this works with MRJARs (it should, but it's untested)
                            JarFile jar = new JarFile(p.toFile());
                            Stream<JarEntry> classFiles;
                            String vStr = System.getProperty("java.version");
                            int vDot = vStr.indexOf('.');
                            // v = The version number, or 1 if the version is <=8
                            int v = Integer.parseInt(vDot == -1 ? vStr : vStr.substring(0, vDot));
                            if (v > 8 && Optional.ofNullable(jar.getManifest().getMainAttributes().getValue("Multi-Release"))
                                    .map(String::toLowerCase)
                                    .map("true"::equals)
                                    .orElse(false))
                            {
                                Map<String, Tuple2<JarEntry, Integer>> entryMap = new HashMap<>();
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
                                                entryMap.compute(name, (k, val) -> val == null || val.e1() < ev ? new Tuple2<>(entry, ev) : val);
                                        }
                                    }
                                    else
                                        entryMap.put(name, new Tuple2<>(entry, 8));
                                }
                                classFiles = entryMap.values().stream().map(Tuple2::e0);
                            }
                            else {
                                classFiles = jar.stream()
                                        .filter(e -> !e.isDirectory() && e.getName().toLowerCase().endsWith(".class") && !e.getName().toUpperCase().startsWith("META-INF"));
                            }

                            return classFiles
                                    .map((FunctionThrowsIOException<JarEntry, byte[]>) (f -> {
                                        InputStream in = jar.getInputStream(f);
                                        ByteArrayOutputStream bufs = new ByteArrayOutputStream();
                                        byte[] buf = new byte[0x400]; // 1kb
                                        int len;
                                        while ((len = in.read(buf)) != -1)
                                            bufs.write(buf, 0, len);
                                        return bufs.toByteArray();
                                    }));
                        }
                        else if (p.toString().endsWith(".class")) {
                            return Stream.of(Files.readAllBytes(p));
                        }
                        else
                            // Somewhere, somehow, this gets wrapped in another UserErrorException (I think). Nevermind, it's fine now
                            throw new UserErrorException("Unable to load classes from `" + p + "' because it is not a .class or .jar file");
                    }))
                    .collect(Collectors.toCollection(LinkedList::new));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }

        Queue<byte[]> q2 = new LinkedList<>();

        Class<?>[] classes = new Class<?>[q1.size()];

        int i = 0;
        while (!q1.isEmpty()) {
            boolean success = false;
            NoClassDefFoundError failCause = null;
            while (!q1.isEmpty()) {
                byte[] b = q1.remove();
                try {
                    classes[i] = defineClass(null, b, 0, b.length);
                    i++;
                    success = true;
                }
                catch (NoClassDefFoundError e) {
                    q2.add(b);
                    failCause = e;
                }
                catch (LinkageError e) {
                    throw new UserErrorException(e);
                }
            }
            if (!success)
                throw new UserErrorException(failCause);
            Queue<byte[]> temp = q1;
            q1 = q2;
            q2 = temp;
        }

        return classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        lock.lock();
        if (isLoading) {
            try {
                loadConditions.computeIfAbsent(name, k -> lock.newCondition()).await();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return loadClass(name);
        }
        else
            return super.findClass(name);
    }
}
