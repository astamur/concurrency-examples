package dev.astamur.concurrency.primitives.other;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Ping-pong where the coordination lives entirely in the queues — there is no {@code synchronized}
 * block, no {@code wait}/{@code notify}, and no shared boolean "turn" flag.
 *
 * <p>Two {@link SynchronousQueue}s act as direct hand-off channels (a {@code put} blocks until a
 * matching {@code take}). The pinger sends the ball on {@code pings} and waits for it to come back on
 * {@code pongs}; the ponger does the mirror. The blocking rendezvous of the queues is what enforces
 * the strict alternation — remove either queue and the example no longer works.
 */
public class PingPongWithQueue {
    private static final Object BALL = new Object();

    private final BlockingQueue<Object> pings = new SynchronousQueue<>(); // ball on its way to the ponger
    private final BlockingQueue<Object> pongs = new SynchronousQueue<>(); // ball on its way back to the pinger

    public void ping() throws InterruptedException {
        System.out.println("Ping - " + Thread.currentThread().getName());
        pings.put(BALL);   // hand the ball to a ponger (blocks until it is taken)
        pongs.take();      // wait for the ball to return
    }

    public void pong() throws InterruptedException {
        pings.take();      // wait for a ball to answer
        System.out.println("Pong - " + Thread.currentThread().getName());
        pongs.put(BALL);   // hand the ball back (blocks until it is taken)
    }

    public static void main(String[] args) throws InterruptedException {
        PingPongWithQueue pingPong = new PingPongWithQueue();

        Thread pinger = new Thread(() -> loopUntilInterrupted(pingPong::ping), "pinger");
        Thread ponger = new Thread(() -> loopUntilInterrupted(pingPong::pong), "ponger");

        pinger.start();
        ponger.start();

        Thread.sleep(100);

        pinger.interrupt();
        ponger.interrupt();
    }

    private interface InterruptibleAction {
        void run() throws InterruptedException;
    }

    private static void loopUntilInterrupted(InterruptibleAction action) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                action.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
