package ru.astamur.concurrency.examples.structure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class DummyWorkQueue {
    private static final Logger log = LoggerFactory.getLogger(DummyWorkQueue.class);

    private final PoolWorker[] threads;
    private final LinkedList<Runnable> queue;
    private volatile boolean shutdownFlag;

    public DummyWorkQueue(int threadsCount) {
        queue = new LinkedList<>();
        threads = new PoolWorker[threadsCount];

        // Start all worker threads
        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new PoolWorker();
            threads[i].start();
        }
    }

    public void execute(Runnable r) {
        if (shutdownFlag) {
            throw new RuntimeException("WorkQueue is going to shutdown. New tasks not accepted");
        }

        synchronized (queue) {
            queue.addLast(r);
            queue.notify();
        }
    }

    /**
     * Sets shutdown flag. With the shutdown flag set to {@code true}, queue doesn't accept any new tasks,
     * it only completes existing ones.
     */
    public void shutdown() {
        shutdownFlag = true;
    }

    /**
     * Sets shutdown flag and interrupts all threads. With the shutdown flag set to {@code true},
     * queue doesn't accept any new tasks. All threads will be force stopped.
     */
    public void shutdownNow() {
        shutdownFlag = true;
        for (PoolWorker thread : threads) {
            thread.interrupt();
        }
    }

    private class PoolWorker extends Thread {
        public void run() {
            Runnable task;

            while (true) {
                // Has this thread been interrupted
                if (this.isInterrupted()) {
                    log.info("'{}': bye bye!", this.getName());
                    return;
                }

                synchronized (queue) {
                    while (queue.isEmpty()) {
                        try {
                            // There won't more tasks if we are going to shutdown
                            if (shutdownFlag) {
                                log.info("'{}': bye bye!", this.getName());
                                return;
                            }

                            // Wait for the new added tasks
                            queue.wait();
                        } catch (InterruptedException e) {
                            log.info("'{}': bye bye!", this.getName());
                            return;
                        }
                    }

                    // Get a task for execution
                    task = queue.removeFirst();
                }

                try {
                    task.run();
                } catch (RuntimeException e) {
                    log.error("Error during task execution", e);
                }
            }
        }
    }
}