package dev.astamur.concurrency.primitives.structure;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleBlockingQueueWithLock<T> {
    private final List<T> list;
    private final int capacity;

    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public SimpleBlockingQueueWithLock(int capacity) {
        list = new LinkedList<>();
        this.capacity = capacity;
    }

    public void offer(T value, long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        lock.lock();

        try {
            while (list.size() == capacity) {
                if (!notFull.await(timeout, timeUnit)) {
                    throw new TimeoutException("Timeout has expired");
                }
            }

            list.add(value);

            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T poll(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        lock.lock();

        try {
            while (list.isEmpty()) {
                if (!notEmpty.await(timeout, timeUnit)) {
                    throw new TimeoutException("Timeout has been expired");
                }
            }

            notFull.signal();

            return list.remove(0);
        } finally {
            lock.unlock();
        }
    }
}
