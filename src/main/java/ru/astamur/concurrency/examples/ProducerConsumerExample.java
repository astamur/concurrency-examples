package ru.astamur.concurrency.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerConsumerExample {
    public static void main(String[] args) throws InterruptedException {
        MessageBox messageBox = new MessageBox(5);
        Thread producer1 = new Thread(new Producer("Producer #1", messageBox, 1000));
        Thread producer2 = new Thread(new Producer("Producer #2", messageBox, 1000));
        Thread consumer = new Thread(new Consumer("Consumer #1", messageBox, 100));

        producer1.start();
        producer2.start();
        consumer.start();
    }

    private static class MessageBox {
        private final static Logger log = LoggerFactory.getLogger(MessageBox.class);

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
            }
        }
    }

    private static class Consumer implements Runnable {
        private final static Logger log = LoggerFactory.getLogger(Consumer.class);

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
            }
        }
    }
}
