package ru.astamur.concurrency.examples.structure;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.assertTrue;

public class SimpleExecutorTest {
    private static final Logger log = LoggerFactory.getLogger(SimpleExecutorTest.class);

    private static final int THREADS_COUNT = 10;
    private static final int EXECUTIONS_COUNT = 1000;

    @Test
    public void testWorkUntilShutdown() throws InterruptedException {
        SimpleExecutor executor = new SimpleExecutor(THREADS_COUNT);
        AtomicInteger counter = new AtomicInteger();

        IntStream.range(0, EXECUTIONS_COUNT)
                .forEach(index -> executor.execute(new Task(index, counter)));

        executor.shutdown();
        executor.awaitTermination();

        assertTrue("Total counter value is incorrect", counter.get() == EXECUTIONS_COUNT);
    }

    @Test
    public void testInterruptWhenShutdownNow() throws InterruptedException {
        SimpleExecutor executor = new SimpleExecutor(THREADS_COUNT);
        AtomicInteger counter = new AtomicInteger();

        IntStream.range(0, EXECUTIONS_COUNT).forEach(index -> executor.execute(new Task(index, counter)));

        executor.shutdownNow();
        executor.awaitTermination();

        assertTrue("Total counter value is incorrect", counter.get() < EXECUTIONS_COUNT);
    }

    private static class Task implements Runnable {
        private String name;
        private AtomicInteger counter;

        Task(int number, AtomicInteger counter) {
            this.name = String.format("Task #%d", number);
            this.counter = counter;
        }

        @Override
        public void run() {
            log.info("'{}' has started", name);
            try {
                Thread.sleep(1);
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            log.info("'{}' has finished", name);
        }
    }

}