package dev.astamur.concurrency.primitives;

import dev.astamur.concurrency.primitives.structure.SimpleBlockingQueueWithLock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Slf4j
public class SimpleBlockingQueueWithLockTest {
    @Test
    public void shouldProcessAllRequests() throws InterruptedException {
        SimpleBlockingQueueWithLock<Integer> blockingQueue = new SimpleBlockingQueueWithLock<>(100);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        ExecutorService offerExecutor = Executors.newFixedThreadPool(10);
        ExecutorService pollExecutor = Executors.newFixedThreadPool(10);

        IntStream.range(0, 10_000).forEach(i -> offerExecutor.execute(() -> {
            try {
                blockingQueue.offer(i, 1, TimeUnit.MINUTES);
            } catch (InterruptedException | TimeoutException e) {
                exceptions.add(e);
            }
        }));

        IntStream.range(0, 10_000).forEach(i -> pollExecutor.execute(() -> {
            try {
                blockingQueue.poll(1, TimeUnit.MINUTES);
            } catch (InterruptedException | TimeoutException e) {
                exceptions.add(e);
            }
        }));

        offerExecutor.shutdown();
        pollExecutor.shutdown();

        if (!offerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            fail("Waiting too long");
        }

        if (!pollExecutor.awaitTermination(1, TimeUnit.MILLISECONDS)) {
            fail("Waiting too long");
        }

        assertThat(exceptions).hasSize(0);
    }
}