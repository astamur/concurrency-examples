package dev.astamur.concurrency.primitives.structure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class SimpleExecutor {
    private static final Logger log = LoggerFactory.getLogger(SimpleExecutor.class);

    private final Worker[] threads;
    private final LinkedList<Runnable> queue = new LinkedList<>();

    // Number of tasks currently being executed (already removed from the queue but not yet finished).
    // Guarded by the {@code queue} monitor so it can be checked together with the queue's emptiness.
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
            throw new IllegalStateException("Executor is going to shutdown. New tasks are not accepted");
        }

        synchronized (queue) {
            queue.addLast(r);
            queue.notifyAll();
        }
    }

    /**
     * Sets shutdown flag. With the shutdown flag set to {@code true}, queue doesn't accept any new tasks,
     * it only completes existing ones.
     */
    public void shutdown() {
        shutdownFlag = true;

        // Wake up idle workers so they can observe the shutdown flag and terminate,
        // and wake up anyone blocked in awaitTermination().
        synchronized (queue) {
            queue.notifyAll();
        }
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
                // Termination is complete only when there is nothing left to run
                // AND nothing currently running. Both are read under the same lock,
                // which closes the "dequeued but not yet counted" race.
                while (!queue.isEmpty() || activeWorkersCounter != 0) {
                    queue.wait();
                }
            } catch (InterruptedException e) {
                log.info("Oops! An interruption! I am not awaiting anymore!", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * An internal executor's worker thread implementation.
     */
    private class Worker extends Thread {
        public void run() {
            while (true) {
                Runnable task;

                synchronized (queue) {
                    while (queue.isEmpty()) {
                        // There won't be new tasks if we are going to shutdown
                        if (shutdownFlag) {
                            log.info("'{}': bye bye!", this.getName());
                            return;
                        }

                        try {
                            // Wait for the newly added tasks
                            queue.wait();
                        } catch (InterruptedException e) {
                            log.info("'{}': bye bye!", this.getName());
                            return;
                        }
                    }

                    // Has this thread been interrupted (e.g. by shutdownNow)?
                    if (this.isInterrupted()) {
                        log.info("'{}': bye bye!", this.getName());
                        return;
                    }

                    // Claim the task and mark it as in-flight while still holding the lock,
                    // so awaitTermination() can never see an empty queue with zero active workers
                    // in the window between dequeue and execution.
                    task = queue.removeFirst();
                    activeWorkersCounter++;
                }

                try {
                    task.run();
                } catch (RuntimeException e) {
                    log.error("Error during task execution", e);
                } finally {
                    synchronized (queue) {
                        activeWorkersCounter--;
                        // A worker finished: awaitTermination() may now be able to proceed.
                        queue.notifyAll();
                    }
                }
            }
        }
    }
}
