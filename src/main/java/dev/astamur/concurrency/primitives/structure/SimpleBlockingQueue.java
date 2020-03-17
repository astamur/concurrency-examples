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
            long timeoutInMillis = timeUnit.toMillis(timeout);
            long start = System.currentTimeMillis();

            while (list.size() == capacity) {
                list.wait(timeoutInMillis);
                checkTimeout(start, timeoutInMillis);
            }

            list.add(value);
            list.notify();
        }
    }

    public T poll(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        synchronized (list) {
            long timeoutInMillis = timeUnit.toMillis(timeout);
            long start = System.currentTimeMillis();

            while (list.isEmpty()) {
                list.wait();
                checkTimeout(start, timeoutInMillis);
            }

            list.notify();

            return list.remove(0);
        }
    }

    private void checkTimeout(long start, long timeout) throws TimeoutException {
        if (System.currentTimeMillis() - start >= timeout) {
            throw new TimeoutException("Timeout has expired");
        }
    }
}
