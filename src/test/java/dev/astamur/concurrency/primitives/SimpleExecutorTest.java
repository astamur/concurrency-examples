package dev.astamur.concurrency.primitives;

import dev.astamur.concurrency.primitives.structure.SimpleExecutor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class SimpleExecutorTest {
    private static final int THREADS_COUNT = 10;
    private static final int EXECUTIONS_COUNT = 1000;

    @Test
    public void testWorkUntilShutdown() {
        SimpleExecutor executor = new SimpleExecutor(THREADS_COUNT);
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch openGate = new CountDownLatch(0); // already open: every task runs to completion

        IntStream.range(0, EXECUTIONS_COUNT)
                .forEach(index -> executor.execute(new Task(index, counter, openGate)));

        executor.shutdown();
        executor.awaitTermination();

        assertThat(counter.get()).isEqualTo(EXECUTIONS_COUNT);
    }

    @Test
    public void testInterruptWhenShutdownNow() {
        SimpleExecutor executor = new SimpleExecutor(THREADS_COUNT);
        AtomicInteger counter = new AtomicInteger();
        // Never released: tasks that reach a worker block on this gate until the worker is interrupted,
        // so no task ever increments the counter. This makes the assertion deterministic instead of
        // racing task completion against shutdownNow().
        CountDownLatch closedGate = new CountDownLatch(1);

        IntStream.range(0, EXECUTIONS_COUNT)
                .forEach(index -> executor.execute(new Task(index, counter, closedGate)));

        executor.shutdownNow();
        executor.awaitTermination();

        assertThat(counter.get()).isEqualTo(0);
    }

    private static class Task implements Runnable {
        private final String name;
        private final AtomicInteger counter;
        private final CountDownLatch gate;

        Task(int number, AtomicInteger counter, CountDownLatch gate) {
            this.name = String.format("Task #%d", number);
            this.counter = counter;
            this.gate = gate;
        }

        @Override
        public void run() {
            log.info("'{}' has started", name);
            try {
                gate.await();
                counter.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            log.info("'{}' has finished", name);
        }
    }
}
