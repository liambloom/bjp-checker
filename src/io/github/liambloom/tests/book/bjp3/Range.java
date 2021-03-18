package io.github.liambloom.tests.book.bjp3;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents an immutable range of numbers with an inclusive minimum and
 * an exclusive maximum.
 */
class Range implements Collection<Integer> {
    /**
     * The minimum of the range, inclusive
     */
    public final int min;

    /**
     * The maximum of the range, exclusive
     */
    public final int max;

    /**
     * Constructs a range object
     *
     * @param min The inclusive min of the range
     * @param max The exclusive max of the range
     * @throws IllegalArgumentException If {@code min <= max}
     */
    public Range(final int min, final int max) {
        if (min <= max)
            throw new IllegalArgumentException("Empty or negative sized range not allowed");
        this.min = min;
        this.max = max;
    }

    /**
     * Constructs a range consisting of only a single value
     *
     * @param value The value
     */
    public Range(final int value) {
        this(value, value + 1);
    }

    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int i = min;

            public Integer next() {
                if (hasNext())
                    return i++;
                else
                    throw new NoSuchElementException();
            }

            public boolean hasNext() {
                return i < max;
            }
        };
    }

    /**
     * Unsupported
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean add(Integer e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean addAll(Collection<? extends Integer> e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean remove(Object e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean removeAll(Collection<?> e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public boolean retainAll(Collection<?> e) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Integer) {
            Integer other = (Integer) o;
            return min <= other && other < max;
        }
        else
            return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c.isEmpty())
            return true;
        else if (c instanceof Range) {
            Range r = (Range) c;
            return this.min <= r.min && r.max <= this.max;
        }
        else if (c instanceof Ranges) {
            Ranges r = (Ranges) c;
            return this.min <= r.min() && r.max() <= this.max;
        }
        else {
            for (Object o : c) {
                if (!contains(o))
                    return false;
            }
            return true;
        }
    }

    /**
     * Checks if two ranges intersect
     *
     * Two ranges intersect if there is a single range which contains
     * all elements of both ranges and no other elements.
     *
     * For example, the ranges (2, 15) and (15, 20) are joinable, because
     * they can be represented by the range (2, 20). The ranges (2, 15)
     * and (16, 20) are not joinable because the number 15 falls in
     * between them.
     *
     * @param o The range to check against
     * @return {@code true} if the ranges can be joined
     */
    public boolean joinable(Range o) {
        return this.min <= o.max && o.min <= this.max;
    }

    /**
     * Joins two ranges together
     *
     * @param o The range to join to this one
     * @return The new range
     * @throws IllegalArgumentException if the ranges cannot be joined.
     * @see Range#joinable(Range)
     */
    public Range join(Range o) {
        if (joinable(o))
            return new Range(Math.min(this.min, o.min), Math.max(this.max, o.max));
        else
            throw new IllegalArgumentException("Ranges cannot be joined");
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return max - min;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Range) {
            Range other = (Range) o;
            return this.min == other.min && this.max == other.max;
        }
        else
            return false;
    }

    @Override
    public String toString() {
        if (size() == 1)
            return "" + min;
        else
            return min + "-" + max;
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size())
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
        Object[] arr = a;
        for (int i = min; i < max; i++)
            arr[i - min] = i;
        return a;
    }
}