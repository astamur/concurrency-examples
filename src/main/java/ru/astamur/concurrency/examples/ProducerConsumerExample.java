package ru.astamur.concurrency.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumerExample {
    public static void main(String[] args) throws InterruptedException {
        Box box = new Box(5);
        Thread producer1 = new Thread(new Producer("Producer #1", box, 1000));
        Thread producer2 = new Thread(new Producer("Producer #2", box, 1000));
        Thread consumer = new Thread(new Consumer("Consumer #1", box, 100));

        producer1.start();
        producer2.start();
        consumer.start();
    }

    private static class Box {
        private final static Logger log = LoggerFactory.getLogger(Box.class);

        private List<String> messages = new ArrayList<>();
        private int capacity;

        Box(int capacity) {
            this.capacity = capacity;
        }

        synchronized String take() {
            while (messages.isEmpty()) {
                try {
                    log.debug("Nothing to consume. Wait");
                    wait();
                } catch (InterruptedException e) {
                    log.error("Interruption. Someone doesn't want to wait", e);
                }
            }
            notifyAll();
            return messages.remove(0);
        }

        synchronized void put(String message) {
            while (messages.size() > capacity) {
                log.debug("No place to put. Wait");
                try {
                    wait();
                } catch (InterruptedException e) {
                    log.error("Interruption. Someone doesn't want to wait", e);
                }
            }
            messages.add(message);
            notifyAll();
        }
    }

    private static class Producer implements Runnable {
        private final static Logger log = LoggerFactory.getLogger(Producer.class);

        private AtomicInteger counter = new AtomicInteger(0);
        private Box box;
        private String name;
        private long delay;

        Producer(String name, Box box, long delay) {
            this.name = name;
            this.box = box;
            this.delay = delay;
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(delay);

                    String message = String.format("message_%d", counter.incrementAndGet());
                    log.debug("{}: produced message '{}'", name, message);
                    box.put(message);
                }
            } catch (InterruptedException e) {
                log.error("Interruption. Stop producing", e);
            }
        }
    }

    private static class Consumer implements Runnable {
        private final static Logger log = LoggerFactory.getLogger(Consumer.class);

        private String name;
        private Box box;
        private long delay;

        Consumer(String name, Box box, long delay) {
            this.name = name;
            this.box = box;
            this.delay = delay;
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(delay);
                    log.info("{}: '{}'", name, box.take());
                }
            } catch (InterruptedException e) {
                log.error("Interruption. Stop consuming", e);
            }
        }
    }
}
