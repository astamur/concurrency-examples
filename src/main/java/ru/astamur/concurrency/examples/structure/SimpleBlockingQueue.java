package ru.astamur.concurrency.examples.structure;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleBlockingQueue<T> {
    private final List<T> list;
    private final int capacity;
    private int size;

    public SimpleBlockingQueue(int capacity) {
        list = new LinkedList<>();
        this.capacity = capacity;
    }

    public void offer(T value, long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        synchronized (list) {
            long timeoutInMillis = timeUnit.toMillis(timeout);
            long start = System.currentTimeMillis();

            while (size == capacity) {
                list.wait(timeoutInMillis);
                checkTimeout(start, timeoutInMillis);
            }

            list.add(value);
            size++;

            list.notify();
        }
    }

    public T poll(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        synchronized (list) {
            long timeoutInMillis = timeUnit.toMillis(timeout);
            long start = System.currentTimeMillis();

            while (size == 0) {
                list.wait();
                checkTimeout(start, timeoutInMillis);
            }

            size--;
            list.notify();

            return list.remove(0);
        }
    }

    private void checkTimeout(long start, long timeout) throws TimeoutException {
        if (System.currentTimeMillis() - start >= timeout) {
            throw new TimeoutException("Timeout has been expired");
        }
    }
}
