package com.codessay.concurrency.primitives.other;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Slf4j
public class CountdownLatchExample {
    public static void main(String[] args) throws InterruptedException {
        final int count = 20;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(count);

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        IntStream.range(0, count).forEach(i -> executorService.execute(() -> {
            try {
                startLatch.await();

                Thread.sleep(1000L);
                log.info("Task has been done");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        }));

        long startTime = System.currentTimeMillis();
        startLatch.countDown();
        endLatch.await();
        log.info("Elapsed time: {} ms", System.currentTimeMillis() - startTime);

        executorService.shutdownNow();
    }
}
