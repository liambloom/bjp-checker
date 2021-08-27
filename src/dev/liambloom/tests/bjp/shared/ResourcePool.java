package dev.liambloom.tests.bjp.shared;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

public class ResourcePool<T> {
    public static final long DEFAULT_MAX_IDLE = 60000;

    private final Timer timer = new Timer(true);
    private final Deque<PoolElement> pool = new ConcurrentLinkedDeque<>();
    private final Supplier<? extends T> supplier;
    private final long maxIdleTime;

    public ResourcePool(Supplier<? extends T> supplier) {
        this(supplier, DEFAULT_MAX_IDLE);
    }

    public ResourcePool(Supplier<? extends T> supplier, long maxIdleTime) {
        this.supplier = supplier;
        this.maxIdleTime = maxIdleTime;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    public T get() {
        // FIXME: A new one is generated MUCH too often, I'm definitely doing something wrong
        return Optional.ofNullable(pool.pollLast())
                .map(PoolElement::take)
                .map(v -> {
                    System.out.println("new item " + v + " generated");
                    return v;
                })
                .orElseGet(supplier);
    }

    public void offer(T e) {
        pool.offerLast(new PoolElement(e));
    }

    private class PoolElement {
        private final T item;
        private final TimerTask task;

        public PoolElement(T item) {
            this.item = item;
            task = new TimerTask() {
                @Override
                public void run() {
                    pool.remove(PoolElement.this);
                }
            };
            timer.schedule(task, maxIdleTime);
        }

        public T take() {
            task.cancel();
            return item;
        }
    }
}
