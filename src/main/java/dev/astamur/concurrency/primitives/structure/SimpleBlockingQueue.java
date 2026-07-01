package dev.astamur.concurrency.primitives.structure;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleBlockingQueue<T> {
    private final List<T> list;
    private final int capacity;

    public SimpleBlockingQueue(int capacity) {
        list = new LinkedList<>();
        this.capacity = capacity;
    }

    public void offer(T value, long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        synchronized (list) {
            long deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);

            while (list.size() == capacity) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new TimeoutException("Timeout has expired");
                }
                list.wait(remaining);
            }

            list.add(value);
            // Producers and consumers wait on the same monitor, so notifyAll() is required:
            // notify() could wake another producer and leave the waiting consumer asleep.
            list.notifyAll();
        }
    }

    public T poll(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        synchronized (list) {
            long deadline = System.currentTimeMillis() + timeUnit.toMillis(timeout);

            while (list.isEmpty()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new TimeoutException("Timeout has expired");
                }
                list.wait(remaining);
            }

            T value = list.remove(0);
            list.notifyAll();
            return value;
        }
    }
}
