package ru.astamur.concurrency.examples.structure;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Slf4j
public class SimpleBlockingQueueTest {
    @Test
    public void shouldBlockOnOffer() {
        SimpleBlockingQueue<String> blockingQueue = new SimpleBlockingQueue<>(1);

        try {
            blockingQueue.offer("success", 1, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            log.error("Failed", e);
            fail("Should not fail while offering");
        }

        try {
            blockingQueue.offer("block", 1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Failed", e);
            fail("Should not be interrupted here");
        } catch (TimeoutException e) {
            // as expected
        }
    }

    @Test
    public void shouldProcessAllRequests() throws InterruptedException {
        SimpleBlockingQueue<Integer> blockingQueue = new SimpleBlockingQueue<>(5);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(10);

        IntStream.range(0, 10_000).forEach(i -> executor.execute(() -> {
            try {
                blockingQueue.offer(i, 10, TimeUnit.SECONDS);
                TimeUnit.MILLISECONDS.sleep(1);
                blockingQueue.poll(10, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException e) {
                exceptions.add(e);
            }
        }));

        executor.shutdown();

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            fail("Waiting too long");
        }


        assertEquals(0, exceptions.size());
    }
}