package dev.liambloom.checker.bjp.api;

import java.util.Deque;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

public class ResourcePool<T> implements Supplier<T> {
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

    @Override
    public T get() {
        return Optional.ofNullable(pool.pollLast())
            .map(PoolElement::take)
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
