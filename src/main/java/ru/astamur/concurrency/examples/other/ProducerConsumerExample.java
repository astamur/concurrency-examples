package ru.astamur.concurrency.examples.other;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumerExample {
    public static void main(String[] args) throws InterruptedException {
        MessageBox messageBox = new MessageBox(1);
        Thread producer1 = new Thread(new Producer("Producer #1", messageBox, 1000));
        Thread producer2 = new Thread(new Producer("Producer #2", messageBox, 1000));
        Thread producer3 = new Thread(new Producer("Producer #3", messageBox, 1000));
        Thread consumer = new Thread(new Consumer("Consumer #1", messageBox, 2000));

        producer1.start();
        producer2.start();
        producer3.start();
        consumer.start();
    }

    @Slf4j
    private static class MessageBox {
        private List<String> messages = new ArrayList<>();
        private int capacity;

        MessageBox(int capacity) {
            this.capacity = capacity;
        }

        synchronized String take() {
            while (messages.isEmpty()) {
                try {
                    log.debug("Nothing to consume. Wait");
                    wait();
                } catch (InterruptedException e) {
                    log.error("Interruption. Someone doesn't want to wait", e);
                    Thread.currentThread().interrupt();
                }
            }
            notifyAll();
            return messages.remove(0);
        }

        synchronized void put(String message) {
            while (messages.size() == capacity) {
                log.debug("No place to put. Wait");
                try {
                    wait();
                } catch (InterruptedException e) {
                    log.error("Interruption. Someone doesn't want to wait", e);
                    Thread.currentThread().interrupt();
                }
            }
            messages.add(message);

            log.debug("MessageBox: received message: '{}' ,size: {}", message, messages.size());

            notifyAll();
        }
    }

    @Slf4j
    private static class Producer implements Runnable {
        private static final AtomicInteger counter = new AtomicInteger(0);
        private MessageBox messageBox;
        private String name;
        private long delay;

        Producer(String name, MessageBox messageBox, long delay) {
            this.name = name;
            this.messageBox = messageBox;
            this.delay = delay;
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(delay);

                    String message = String.format("message_%d", counter.incrementAndGet());

                    log.debug("{}: produced message '{}'", name, message);

                    messageBox.put(message);
                }
            } catch (InterruptedException e) {
                log.error("Interruption. Stop producing", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Slf4j
    private static class Consumer implements Runnable {
        private String name;
        private MessageBox messageBox;
        private long delay;

        Consumer(String name, MessageBox messageBox, long delay) {
            this.name = name;
            this.messageBox = messageBox;
            this.delay = delay;
        }

        public void run() {
            try {
                while (true) {
                    Thread.sleep(delay);
                    log.info("{}: '{}'", name, messageBox.take());
                }
            } catch (InterruptedException e) {
                log.error("Interruption. Stop consuming", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
