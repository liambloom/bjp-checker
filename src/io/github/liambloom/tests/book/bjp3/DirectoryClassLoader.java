package io.github.liambloom.tests.book.bjp3;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

class DirectoryClassLoader {
    private static final Pattern PATH_SEGMENT = Pattern.compile("\\" + File.separatorChar + "[a-zA-Z_$][\\w$]*");

    public static Class<?>[] loadClassesHere() throws ClassNotFoundException, MalformedURLException {
        return loadClasses(new File(System.getProperty("user.dir")));
    }

    public static Class<?>[] loadClasses(File here) throws ClassNotFoundException, MalformedURLException {
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
                // FIXME: Doesn't work with nested/inner classes
                classes[i] = loader.loadClass(file.replace(".class", "").replace(loader.getURLs()[0].toString(), "").replace("/", "."));
                break;
            }
            assert classes[i] != null;
        }

        return classes;
    }

    private static void getFiles(final File parent, final List<String> files, final List<URL> dirs) throws MalformedURLException {
        if (parent.isFile())
            files.add(parent.toURI().toURL().toString());
        else {
            int len = files.size();
            for (File f : parent.listFiles()) {
                if (f.isDirectory())
                    getFiles(f, files, dirs);
                else if (f.getName().endsWith(".class"))
                    files.add(f.toURI().toURL().toString());
            }
            if (len != files.size())
                dirs.add(parent.toURI().toURL());
        }
    }
}

