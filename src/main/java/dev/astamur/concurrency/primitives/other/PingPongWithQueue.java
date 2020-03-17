package dev.astamur.concurrency.primitives.other;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

public class PingPongWithQueue {
    private final BlockingQueue<String> pings = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> pongs = new LinkedBlockingQueue<>();
    private volatile boolean state = true;

    public synchronized void ping() {
        try {
            pings.offer("Ping");

            while (!state) {
                wait();
            }

            System.out.println(pongs.take() + " - " + Thread.currentThread().getName());
            state = false;
            notify();
        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + " was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void pong() {
        try {
            pings.offer("Pong");

            while (state) {
                wait();
            }

            System.out.println(pings.take() + " - " + Thread.currentThread().getName());
            state = true;
            notify();
        } catch (InterruptedException e) {
            System.out.println(Thread.currentThread().getName() + " was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PingPong pingPong = new PingPong();
        Thread[] pingers = new Thread[10];
        Thread[] pongers = new Thread[10];

        IntStream.range(0, 10).forEach(i -> {
            pingers[i] = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    pingPong.ping();
                }
            });

            pongers[i] = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    pingPong.pong();
                }
            });

            pingers[i].start();
            pongers[i].start();
        });

        Thread.sleep(100);

        Arrays.stream(pingers).forEach(Thread::interrupt);
        Arrays.stream(pongers).forEach(Thread::interrupt);
    }
}