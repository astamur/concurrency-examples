package ru.astamur.concurrency.examples.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class ProducerConsumerWithLocksExample {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        MessageBox messageBox = new MessageBox(5);

        executor.execute(new Consumer("Consumer #1", messageBox));

        IntStream.range(1, 6)
                .forEach(i -> executor.execute(new Producer("Producer #" + i, messageBox)));
    }

    private static class MessageBox {
        private final static Logger log = LoggerFactory.getLogger(MessageBox.class);

        private List<String> messages = new ArrayList<>();
        private int capacity;
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();

        MessageBox(int capacity) {
            this.capacity = capacity;
        }

        String take() {
            try {
                lock.lock();
                while (messages.isEmpty()) {
                    try {
                        log.debug("Nothing to consume. Wait");
                        condition.await();
                    } catch (InterruptedException e) {
                        log.error("Interruption. Someone doesn't want to wait", e);
                    }
                }
                condition.signal();
                return messages.remove(0);
            } finally {
                lock.unlock();
            }
        }

        void put(String message) {
            try {
                lock.lock();
                while (messages.size() > capacity) {
                    log.debug("No place to put. Wait");
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        log.error("Interruption. Someone doesn't want to wait", e);
                    }
                }
                messages.add(message);
                condition.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    private static class Producer implements Runnable {
        private final static Logger log = LoggerFactory.getLogger(Producer.class);

        private AtomicInteger counter = new AtomicInteger(0);
        private MessageBox messageBox;
        private String name;

        Producer(String name, MessageBox messageBox) {
            this.name = name;
            this.messageBox = messageBox;
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000));

                    String message = String.format("message_%d", counter.incrementAndGet());
                    log.debug("{}: produced message '{}'", name, message);
                    messageBox.put(message);
                }
            } catch (InterruptedException e) {
                log.error("Interruption. Stop producing", e);
            }
        }
    }

    private static class Consumer implements Runnable {
        private final static Logger log = LoggerFactory.getLogger(Consumer.class);

        private String name;
        private MessageBox messageBox;

        Consumer(String name, MessageBox messageBox) {
            this.name = name;
            this.messageBox = messageBox;
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(200));
                    log.info("{}: '{}'", name, messageBox.take());
                }
            } catch (InterruptedException e) {
                log.error("Interruption. Stop consuming", e);
            }
        }
    }
}
