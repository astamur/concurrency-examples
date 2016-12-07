package ru.astamur.concurrency.examples.structure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class SimpleExecutor {
    private static final Logger log = LoggerFactory.getLogger(SimpleExecutor.class);

    private final Worker[] threads;
    private final LinkedList<Runnable> queue = new LinkedList<>();
    private final Object activeWorkersCounterLock = new Object();
    private int activeWorkersCounter;

    private volatile boolean shutdownFlag;
    private volatile boolean forceShutdownFlag;

    public SimpleExecutor(int threadsCount) {
        threads = new Worker[threadsCount];

        // Start all worker threads
        for (int i = 0; i < threadsCount; i++) {
            threads[i] = new Worker();
            threads[i].start();
        }
    }

    public void execute(Runnable r) {
        if (shutdownFlag) {
            throw new RuntimeException("Executor is going to shutdown. New tasks are not accepted");
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
     * queue doesn't accept any new tasks. All threads will be force interrupted.
     */
    public void shutdownNow() {
        shutdownFlag = true;
        forceShutdownFlag = true;

        for (Worker thread : threads) {
            thread.interrupt();
        }
    }

    /**
     * This method blocks the calling thread while all tasks from the queue are done.
     * If {@link SimpleExecutor#shutdownNow()} method was previously called then method returns immediately.
     */
    public void awaitTermination() {
        if (!shutdownFlag) {
            throw new IllegalStateException("Executor is not going to shutdown!");
        }

        if (forceShutdownFlag) {
            return;
        }

        synchronized (queue) {
            try {
                while (!queue.isEmpty()) {
                    queue.wait();
                }

                synchronized (activeWorkersCounterLock) {
                    while (activeWorkersCounter != 0) {
                        activeWorkersCounterLock.wait();
                    }
                }
            } catch (InterruptedException e) {
                log.info("Oops! An interruption! I am not awaiting anymore!", e);
            }
        }
    }

    private void startWork() {
        synchronized (activeWorkersCounterLock) {
            activeWorkersCounter++;
            activeWorkersCounterLock.notify();
        }
    }

    private void completeWork() {
        synchronized (activeWorkersCounterLock) {
            activeWorkersCounter--;
            activeWorkersCounterLock.notify();
        }
    }

    /**
     * An internal executor's worker thread implementation.
     */
    private class Worker extends Thread {
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
                            // There won't be new tasks if we are going to shutdown
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
                    queue.notify();
                }

                try {
                    startWork();
                    task.run();
                } catch (RuntimeException e) {
                    log.error("Error during task execution", e);
                } finally {
                    completeWork();
                }
            }
        }
    }
}