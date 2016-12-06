package ru.astamur.concurrency.examples.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.astamur.concurrency.examples.structure.DummyWorkQueue;

import java.util.stream.IntStream;

public class DummyWorkQueueExample {
    public static void main(String[] args) {
        DummyWorkQueue queue = new DummyWorkQueue(5);

        IntStream.range(0, 100).forEach(index -> queue.execute(new Task(index)));

        queue.shutdown();
        //queue.shutdownNow();
    }

    private static class Task implements Runnable {
        private static final Logger log = LoggerFactory.getLogger(Task.class);

        private String name;

        Task(int number) {
            name = String.format("task#%d", number);
        }

        @Override
        public void run() {
            log.info("'{}' has started", name);
            try {
                // Do some stuff
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            log.info("'{}' has finished", name);
        }
    }
}
