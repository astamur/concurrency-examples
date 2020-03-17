package dev.astamur.concurrency.primitives;

import dev.astamur.concurrency.primitives.structure.BoundedHashSet;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class BoundedHashSetTest {
    @Test
    public void shouldBeValidBound() throws InterruptedException {
        BoundedHashSet<String> set = new BoundedHashSet<>(5);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        IntStream.range(0, 1000).forEach(i -> executor.execute(() -> {
            String key = String.format("key_%d", i);

            try {
                set.add(key);

                Thread.sleep(10);

                set.remove(key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(set.size()).isEqualTo(0);
    }
}
