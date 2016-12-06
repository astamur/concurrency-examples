package ru.astamur.concurrency.examples.structure;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;


public class AtomicDoubleTest {
    private static final Logger log = LoggerFactory.getLogger(AtomicDoubleTest.class);

    @Test
    public void testIncrementAndGet() throws Exception {
        int increaseCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(10);

        AtomicDouble atomicDouble = new AtomicDouble();

        IntStream.range(0, increaseCount).forEach(i -> executor.execute(() ->
                log.info("New value: {}", atomicDouble.incrementAndGet())));

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals("", new Double(increaseCount), new Double(atomicDouble.get()));
    }
}