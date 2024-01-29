package dev.astamur.concurrency.primitives;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Test {
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
        volatile int val;
        volatile boolean ready;

        synchronized void write() {
            try {
                for (var i = 0; i < 10; i++) {
                    while (ready) {
                        System.out.printf("Writer. Thread: %s. Counter: %d. State: %s\n",
                            Thread.currentThread().getName(), val, "BEFORE_WAIT");
                        wait();
                    }

                    val++;
                    ready = true;
                    notify();
                    System.out.printf("Writer. Thread: %s. Counter: %d. State: %s\n", Thread.currentThread().getName(), val, "AFTER_WAIT");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized void read() {
            for (var i = 0; i < 100; i++) {
                while (!ready) {
                    try {
                        System.out.printf("Reader. Thread: %s. Counter: %d. State: %s\n", Thread.currentThread().getName(), val,
                            "BEFORE_WAIT");
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                System.out.printf("Reader. Thread: %s. Counter: %d. State: %s\n", Thread.currentThread().getName(), val, "AFTER_WAIT");
                ready = false;
                notify();
            }
        }
    }
}
