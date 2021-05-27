package dev.liambloom.tests.book.bjp.checker;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class SecureGlobClassLoader extends ClassLoader {

    private final SortedMap<String, LazyClass> classes = new TreeMap<>();

    public SecureGlobClassLoader(Glob glob) throws IOException {
        this(glob, getSystemClassLoader());
    }

    public SecureGlobClassLoader(Glob glob, ClassLoader parent) throws IOException {
        super(parent);
        try {
            glob.files()
                    .map(File::toPath)
                    .map((FunctionThrowsIOException<Path, Path>) Glob::readSymbolicLink)
                    .flatMap((FunctionThrowsIOException<Path, Stream<? extends ClassSource>>) (p -> {
                        if (p.toString().endsWith(".jar"))
                            return CompressedClassSource.allSources(new JarFile(p.toFile())).stream();
                        else if (p.toString().endsWith(".class"))
                            return Stream.of(new PathClassSource(p));
                        else
                            throw new UserErrorException("Unable to load classes from `" + p + "' because it is not a .class or .jar file");
                    }))
                    .forEach((ConsumerThrowsIOException<ClassSource>) (s -> classes.merge(new StringBuilder(s.path()).reverse().toString(), new LazyClass(s.bytes()), (v1, v2) -> {
                        throw new UserErrorException("Duplicate class path " + s.path());
                    })));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public Class<?>[] loadAll() {
        System.out.println(classes.size());
        return classes.values()
                .stream()
                .map((FunctionThrowsIOException<LazyClass, Class<?>>) c -> c.get(null))
                //.peek(System.out::println)
                .toArray(Class<?>[]::new);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        StringBuilder nameMut = new StringBuilder(name).reverse();
        String from = nameMut.toString();
        nameMut.setCharAt(name.length() - 1, (char) (nameMut.charAt(name.length() - 1) + 1));
        String to = nameMut.toString();
        for (LazyClass clazz : classes.subMap(from, to).values()) {
            try {
                return clazz.get(name);
            }
            catch (NoClassDefFoundError ignored) {}
        }
        return super.findClass(name);
    }

    private class LazyClass {
        private Class<?> clazz = null;
        private byte[] bytes;

        public LazyClass(byte[] bytes) {
            this.bytes = bytes;
        }

        public synchronized Class<?> get(String name) {
            if (clazz == null) {
                clazz = defineClass(name, bytes, 0, bytes.length);
                bytes = null;
            }
            else if (name != null && !clazz.getName().equals(name))
                throw new NoClassDefFoundError(name);
            return clazz;
        }
    }
}

interface ClassSource {
    String path();
    byte[] bytes() throws IOException;
}

class PathClassSource implements ClassSource {
    private final Path p;

    public PathClassSource(Path p) {
        this.p = p.toAbsolutePath();
    }

    @Override
    public String path() {
        return p.toString().substring(0, p.toString().length() - 6).replace(File.separatorChar, '.');
    }

    @Override
    public byte[] bytes() throws IOException {
        return Files.readAllBytes(p);
    }
}

class CompressedClassSource implements ClassSource {
    public static final Pattern MR_CLASS = Pattern.compile("META-INF/version/\\d+/");
    static final int JAVA_VERSION;

    static {
        String versionString = System.getProperty("java.version");
        String majorVersionString;
        if (versionString.startsWith("1."))
            majorVersionString = versionString.substring(2, 3);
        else {
            int vDot = versionString.indexOf('.');
            majorVersionString = vDot == -1 ? versionString : versionString.substring(0, vDot);
        }
        JAVA_VERSION = Integer.parseInt(majorVersionString);
    }

    private final ZipEntry entry;
    private final JarFile jar;
    private final boolean entryIsMr;

    private static Optional<CompressedClassSource> construct(JarFile jar, ZipEntry entry, boolean jarIsMr) {
        boolean entryIsMr = MR_CLASS.matcher(entry.getName()).lookingAt();

        return jarIsMr || !entryIsMr ? Optional.of(new CompressedClassSource(jar, entry, entryIsMr)).filter(s -> s.version() <= JAVA_VERSION) : Optional.empty();
    }

    private CompressedClassSource(JarFile jar, ZipEntry entry, boolean entryIsMr) {
        this.entry = entry;
        this.jar = jar;
        this.entryIsMr = entryIsMr;
    }

    @Override
    public String path() {
        String p = entry.getName();
        if (entryIsMr)
            p = p.substring(p.indexOf('/', 17));
        return p.substring(0, p.length() - 6).replace('/', '.');
    }

    @Override
    public byte[] bytes() throws IOException {
        InputStream stream = jar.getInputStream(entry);
        ByteArrayOutputStream bufs = new ByteArrayOutputStream();
        byte[] buf = new byte[1024]; // 1kb
        int len;
        while ((len = stream.read(buf)) != -1)
            bufs.write(buf, 0, len);
        return bufs.toByteArray();
    }

    public static Collection<CompressedClassSource> allSources(JarFile jar) throws IOException {
        boolean isMrJar = isMrJar(jar);
        Enumeration<JarEntry> entries = jar.entries();
        Map<String, CompressedClassSource> entryMap = new HashMap<>();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.toLowerCase().endsWith(".class"))
                continue;
            CompressedClassSource.construct(jar, entry, isMrJar)
                    .ifPresent(src -> entryMap.compute(src.path(), (BiFunctionThrowsIOException<String, CompressedClassSource, CompressedClassSource>)
                            (k, val) -> val == null || val.version() < src.version() ? src : val));
        }
        return entryMap.values();
    }

    public int version() {
        String name = entry.getName();
        return entryIsMr ? Integer.parseInt(name.substring(17, name.indexOf('/', 17))) : 8;
    }

    private static boolean isMrJar(JarFile jar) throws IOException {
        return JAVA_VERSION > 8 && Optional.ofNullable(jar.getManifest().getMainAttributes().getValue("Multi-Release"))
                .map(String::toLowerCase)
                .map("true"::equals)
                .orElse(false);
    }
}

