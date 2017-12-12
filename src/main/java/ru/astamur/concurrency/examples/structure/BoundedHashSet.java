package ru.astamur.concurrency.examples.structure;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class BoundedHashSet<T> {
    private final Set<T> set;
    private final Semaphore semaphore;

    public BoundedHashSet(int bound) {
        set = Collections.synchronizedSet(new HashSet<T>());
        semaphore = new Semaphore(bound);
    }

    public boolean add(T element) throws InterruptedException {
        semaphore.acquire();
        boolean isAdded = false;

        try {
            isAdded = set.add(element);
            return isAdded;
        } finally {
            if (!isAdded) {
                semaphore.release();
            }
        }
    }

    public boolean remove(T element) {
        boolean isRemoved = set.remove(element);

        if (isRemoved) {
            semaphore.release();
        }

        return isRemoved;
    }

    public int size() {
        return set.size();
    }
}