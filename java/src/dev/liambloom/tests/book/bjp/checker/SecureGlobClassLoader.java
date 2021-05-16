package dev.liambloom.tests.book.bjp.checker;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SecureGlobClassLoader extends ClassLoader {
    private Glob glob;

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
                            Stream.Builder<byte[]> builder = Stream.builder();
                            ZipInputStream zip = new ZipInputStream(new FileInputStream(p.toFile()));
                            ZipEntry entry;
                            byte[] buf = new byte[1024];
                            while ((entry = zip.getNextEntry()) != null) {
                                ByteArrayOutputStream bufs = new ByteArrayOutputStream();
                                if (entry.isDirectory() || !entry.getName().endsWith(".class"))
                                    continue;
                                int len;
                                while ((len = zip.read(buf)) != -1)
                                    bufs.write(buf, 0, len);
                                builder.add(bufs.toByteArray());
                            }
                            return builder.build();
                        }
                        else if (p.toString().endsWith(".class")) {
                            return Stream.of(Files.readAllBytes(p));
                        }
                        else
                            // FIXME: Somewhere, somehow, this gets wrapped in another UserErrorException (I think)
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
}
