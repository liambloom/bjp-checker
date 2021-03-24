package io.github.liambloom.tests.book.bjp3;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a {@link Collection} of ranges.
 *
 * This is represented internally with a tree.
 */
class Ranges {
    private Range value;
    private Ranges left;
    private Ranges right;

    private class RangesAsIntegerSet implements Set<Integer> {
        @Override
        public boolean add(Integer e) {
            return asRangeCollection().add(new Range(e));
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            boolean r = false;
            for (Integer e : c) {
                if (add(e))
                    r = true;
            }
            return r;
        }

        @Override
        public boolean remove(Object o) {
            // TODO
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            // TODO
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            // TODO
        }

        @Override
        public void clear() {
            Ranges.this.clear();
        }

        @Override
        public boolean contains(Object o) {
            // TODO
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            // TODO
        }

        @Override
        public int size() {
            // TODO
        }

        @Override
        public boolean isEmpty() {
            return Ranges.this.isEmpty();
        }

        @Override
        public Iterator<Integer> iterator() {
            // TODO
        }

        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @Override
        public <T> T[] toArray(T[] a) {
            // TODO
        }

        @Override
        public String toString() {
            return Ranges.this.toString();
        }

        @Override
        public boolean equals(Object o) {
            return Ranges.this.equals(o);
        }

        private Ranges getRanges() {
            return Ranges.this;
        }
    };

    public Set<Integer> asIntegerSet() {
        return new RangesAsIntegerSet();
    }

    private class RangesAsRangeCollection implements Collection<Range> {
        @Override
        public boolean add(Range r) {
            if (value == null){
                value = r;
                return true;
            }
            else if (value.contains(r))
                return false;
            else if (r.joinable(value)) {
                value = value.join(r);
                if (left != null) {
                    value = left.tryJoinRight(value);
                    if (left.value == null)
                        left = null;
                }
                if (right != null) {
                    value = right.tryJoinLeft(value);
                    if (right.value == null)
                        right = null;
                }
                return true;
            }
            else if (r.max < value.min)
                return left.asRangeCollection().add(r);
            else if (r.min > value.max)
                return right.asRangeCollection().add(r);
            else
                throw new IllegalStateException("A mistake was made when comparing ranges");
        }

        @Override
        public boolean addAll(Collection<? extends Range> c) {
            boolean r = false;
            for (Range e : c) {
                if (add(e))
                    r = true;
            }
            return r;
        }

        @Override
        public boolean remove(Object o) {
            // TODO
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            // TODO
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            // TODO
        }

        @Override
        public void clear() {
            Ranges.this.clear();
        }

        @Override
        public boolean contains(Object o) {
            // TODO
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            // TODO
        }

        @Override
        public int size() {
            // TODO
        }

        @Override
        public boolean isEmpty() {
            return Ranges.this.isEmpty();
        }

        @Override
        public Iterator<Range> iterator() {
            // TODO
        }

        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @Override
        public <T> T[] toArray(T[] a) {
            // TODO
        }

        @Override
        public String toString() {
            return Ranges.this.toString();
        }

        @Override
        public boolean equals(Object o) {
            return Ranges.this.equals(o);
        }

        private Ranges getRanges() {
            return Ranges.this;
        }
    };

    public Collection<Range> asRangeCollection() {
        return new RangesAsRangeCollection();
    }

    /**
     * Tries to join this {@code Range} with the one passed in, which should
     * be to its right. Calling this method is does require some work on the
     * part of the callers, as it implements the {@code x = change(x)} pattern,
     * and needs to be able to set itself to null. An example usage would be:
     *
     * <pre>
     * {@code
     * value = left.tryJoinRight(value);
     * if (left.value == null)
     *     left = null;
     * }
     * </pre>
     *
     * @param root The element to the right of {@code this} to join
     * @return The new value of {@code root}
     */
    private Range tryJoinRight(Range root) {
        if (root.joinable(value)) {
            root = root.join(value);

            // this = left
            if (left == null)
                value = null;
            else {
                value = left.value;
                right = left.right;
                left = left.left;
            }
        }
        else if (right != null) {
            root = right.tryJoinRight(root);
            if (right.value == null)
                right = null;
        }

        return root;
    }

    /**
     * Tries to join this {@code Range} with the one passed in, which should
     * be to its left. Calling this method is does require some work on the
     * part of the callers, as it implements the {@code x = change(x)} pattern,
     * and needs to be able to set itself to null. An example usage would be:
     *
     * <pre>
     * {@code
     * value = right.tryJoinLeft(value);
     * if (right.value == null)
     *     right = null;
     * }
     * </pre>
     *
     * @param root The element to the left of {@code this} to join
     * @return The new value of {@code root}
     */
    private Range tryJoinLeft(Range root) {
        if (root.joinable(value)) {
            root = root.join(value);

            // this = left
            if (right == null)
                value = null;
            else {
                value = right.value;
                left = right.left;
                right = right.right;
            }
        }
        else if (left != null) {
            root = left.tryJoinRight(root);
            if (left.value == null)
                left = null;
        }

        return root;
    }

    public void clear() {
        this.value = null;
        this.left = null;
        this.right = null;
    }

    public boolean isEmpty() {
        return value != null;
    }

    /**
     * @return The minimum value in the minimum range
     * @throws IllegalStateException if range is empty
     */
    public Integer min() {
        if (value == null)
            throw new IllegalStateException("Cannot call min on empty range");
        if (left == null)
            return value.min;
        else
            return left.min();
    }

    /**
     * @return The maximum value in the minimum range
     * @throws IllegalStateException if range is empty
     */
    public Integer max() {
        if (value == null)
            throw new IllegalStateException("Cannot call min on empty range");
        if (right == null)
            return value.max;
        else
            return right.max();
    }

    @Override
    public String toString() {
        // TODO
    }

    @Override
    public boolean equals(Object o) {
        // TODO
    }
}
