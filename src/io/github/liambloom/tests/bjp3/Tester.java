package io.github.liambloom.tests.bjp3;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.net.*;

public class Tester {
    private static final Pattern PATH_SEGMENT = Pattern.compile("\\" + File.separatorChar + "[a-zA-Z_\\$][\\w\\$]*");

    private Tester() {}

    public static void main(String[] args) throws MalformedURLException {

        for (Class<?> c : loadClasses()) {
            System.out.println(c.getName());
        }
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