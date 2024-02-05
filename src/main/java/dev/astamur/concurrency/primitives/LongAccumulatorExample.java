package dev.astamur.concurrency.primitives;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.stream.IntStream;

public class LongAccumulatorExample {
    public static void main(String[] args) throws InterruptedException {
        var accumulator = new LongAccumulator(Long::sum, 0); // Same as LongAdder if (x + y) and 0 as an initial value
        var executor = Executors.newFixedThreadPool(10);

        IntStream.range(0, 1_000_000).forEach(i -> executor.execute(() -> accumulator.accumulate(1)));

        executor.shutdown();

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        System.out.println("Value: " + accumulator.get());
    }
}