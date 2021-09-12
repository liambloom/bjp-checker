package dev.liambloom.checker.bjp.api;

import java.lang.reflect.*;
import java.util.*;

public record Targets(Set<Class<?>> classes,
                      Set<Constructor<?>> constructors,
                      Set<Method> methods,
                      Set<Field> fields)
    implements Set<AnnotatedElement> {
    public Targets() {
        this(
            Collections.synchronizedSet(new HashSet<>()),
            Collections.synchronizedSet(new HashSet<>()),
            Collections.synchronizedSet(new HashSet<>()),
            Collections.synchronizedSet(new HashSet<>())
        );
    }

    @Override
    public synchronized boolean add(AnnotatedElement element) {
        if (element instanceof Class<?> clazz)
            return classes.add(clazz);
        else if (element instanceof Constructor<?> constructor)
            return constructors.add(constructor);
        else if (element instanceof Method method)
            return methods.add(method);
        else if (element instanceof Field field)
            return fields.add(field);
        else
            throw new ClassCastException(element.getClass().getName() + " cannot be converted to any of Class, Constructor, Method, or Field");
    }

    @Override
    public synchronized boolean addAll(Collection<? extends AnnotatedElement> c) {
        boolean r = false;
        for (AnnotatedElement e : c)
            r |= add(e);
        return r;
    }

    @Override
    public synchronized boolean remove(Object o) {
        return classes.remove(o) | constructors.remove(o) | methods.remove(o) | fields.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e))
                return false;
        }
        return true;
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return classes.retainAll(c) | constructors.retainAll(c) | methods.retainAll(c) | fields.retainAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return classes.removeAll(c) | constructors.removeAll(c) | methods.removeAll(c) | fields.removeAll(c);
    }

    @Override
    public void clear() {
        classes.clear();
        constructors.clear();
        methods.clear();
        fields.clear();
    }

    public synchronized int size() {
        return classes.size() + constructors.size() + methods.size() + fields.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return classes.isEmpty() && constructors.isEmpty() && methods.isEmpty() && fields.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return classes.contains(o) || constructors.contains(o) || methods.contains(o) || fields.contains(o);
    }

    @Override
    public synchronized Iterator<AnnotatedElement> iterator() {
        return new Iterator<>() {
            @SuppressWarnings("unchecked")
            final Iterator<? extends AnnotatedElement>[] inner = (Iterator<? extends AnnotatedElement>[]) new Iterator[]{
                classes.iterator(),
                constructors.iterator(),
                methods.iterator(),
                fields.iterator()
            };
            int i = 0;

            @Override
            public boolean hasNext() {
                for (int j = i; j < inner.length; j++) {
                    if (inner[j].hasNext())
                        return true;
                }
                return false;
            }

            @Override
            public AnnotatedElement next() {
                for (; i < inner.length; i++) {
                    if (inner[i].hasNext())
                        return inner[i].next();
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                inner[i].remove();
            }
        };
    }

    @Override
    public synchronized Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    @SuppressWarnings({ "unchecked"/*, "SuspiciousSystemArraycopy"*/ })
    public synchronized <T> T[] toArray(T[] a) {
        if (a.length <= size())
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
        int i = 0;
        for (Set<? extends AnnotatedElement> set : new Set[]{ classes, constructors, methods, fields }) {
            for (AnnotatedElement e : set)
                a[i++] = (T) e;
        }
        /*System.arraycopy(classes.toArray(), 0, a, 0, classes.size());
        System.arraycopy(constructors.toArray(), 0, a, classes.size(), constructors.size());
        System.arraycopy(methods.toArray(), 0, a, constructors.size(), methods.size());
        System.arraycopy(fields.toArray(), 0, a, methods.size(), fields.size());*/
        return a;
    }
}
