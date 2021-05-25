package dev.liambloom.tests.book.bjp.checker;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
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

    private final SortedMap<String, LazyClass> classes;

    public SecureGlobClassLoader(Glob glob) throws IOException {
        this(glob, getSystemClassLoader());
    }

    public SecureGlobClassLoader(Glob glob, ClassLoader parent) throws IOException {
        super(parent);
        try {
            classes = glob.files()
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
                    s -> new LazyClass(s.stream()),
                    (v1, v2) -> { throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2)); },
                    TreeMap<String, LazyClass>::new));
        }
        catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public Class<?>[] loadAll() {
        return classes.values()
                .stream()
                .map((FunctionThrowsIOException<LazyClass, Class<?>>) c -> c.get(null))
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
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            catch (NoClassDefFoundError ignored) {}
        }
        return super.findClass(name);
    }

    private class LazyClass {
        private Class<?> clazz = null;
        private InputStream stream;

        public LazyClass(InputStream stream) {
            this.stream = stream;
        }

        public Class<?> get(String name) throws IOException {
            if (clazz == null) {
                ByteArrayOutputStream bufs = new ByteArrayOutputStream();
                byte[] buf = new byte[0x400]; // 1kb
                int len;
                while ((len = stream.read(buf)) != -1)
                    bufs.write(buf, 0, len);
                clazz = defineClass(name, bufs.toByteArray(), 0, bufs.size());
                stream = null;
            }
            else if (name != null && !clazz.getName().equals(name))
                throw new NoClassDefFoundError(name);
            return clazz;
        }
    }
}

class ClassSource {
    private final int version;
    private final InputStream stream;
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

    public InputStream stream() {
        return stream;
    }

    public String name() {
        return name;
    }
}

