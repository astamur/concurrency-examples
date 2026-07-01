package dev.astamur.concurrency.primitives;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class HolderExampleWithLocks {
    public static void main(String[] args) throws InterruptedException {
        var val = new Holder();
        var executor = Executors.newFixedThreadPool(11);

        IntStream.range(0, 10).forEach(i -> executor.execute(val::write));
        executor.execute(val::read);

        executor.shutdown();

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        System.out.println("Counter: " + val.val);
    }

    private static class Holder {
        private final Lock lock = new ReentrantLock();
        private final Condition read = lock.newCondition();
        private final Condition written = lock.newCondition();
        int val;
        boolean isWritten;

        void write() {
            for (var i = 0; i < 10; i++) {
                lock.lock();
                try {
                    while (isWritten) {
                        read.await();
                    }

                    val++;
                    System.out.printf("Writer. Thread: %s. Counter: %d\n", Thread.currentThread().getName(), val);

                    isWritten = true;
                    written.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    lock.unlock();
                }
            }
        }

        void read() {
            for (var i = 0; i < 100; i++) {
                lock.lock();
                try {
                    while (!isWritten) {
                        written.await();
                    }

                    System.out.printf("Reader. Thread: %s. Counter: %d\n", Thread.currentThread().getName(), val);

                    isWritten = false;
                    read.signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
