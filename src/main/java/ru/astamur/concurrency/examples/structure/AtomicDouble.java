package ru.astamur.concurrency.examples.structure;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicDouble extends Number {
    private AtomicReference<Double> value;

    public AtomicDouble() {
        this(0.0);
    }

    public AtomicDouble(double initVal) {
        value = new AtomicReference<>(initVal);
    }

    public double get() {
        return value.get();
    }

    public void set(double newVal) {
        value.set(newVal);
    }

    public boolean compareAndSet(double expect, double update) {
        Double origVal, newVal;
        newVal = update;

        while (true) {
            origVal = value.get();

            if (Double.compare(origVal, expect) == 0) {
                if (value.compareAndSet(origVal, newVal))
                    return true;
            } else {
                return false;
            }
        }
    }

    public boolean weakCompareAndSet(double expect, double update) {
        return compareAndSet(expect, update);
    }

    public double getAndSet(double setVal) {
        Double origVal, newVal;
        newVal = setVal;

        while (true) {
            origVal = value.get();

            if (value.compareAndSet(origVal, newVal))
                return origVal;
        }
    }

    public double getAndAdd(double delta) {
        Double origVal, newVal;

        while (true) {
            origVal = value.get();
            newVal = origVal + delta;

            if (value.compareAndSet(origVal, newVal))
                return origVal;
        }
    }

    public double addAndGet(double delta) {
        Double origVal, newVal;

        while (true) {
            origVal = value.get();
            newVal = origVal + delta;

            if (value.compareAndSet(origVal, newVal))
                return newVal;
        }
    }

    public double getAndIncrement() {
        return getAndAdd(1.0);
    }

    public double getAndDecrement() {
        return getAndAdd(-1.0);
    }

    public double incrementAndGet() {
        return addAndGet(1.0);
    }

    public double decrementAndGet() {
        return addAndGet(-1.0);
    }

    public double getAndMultiply(double multiple) {
        Double origVal, newVal;

        while (true) {
            origVal = value.get();
            newVal = origVal * multiple;

            if (value.compareAndSet(origVal, newVal))
                return origVal;
        }
    }

    public double multiplyAndGet(double multiple) {
        Double origVal, newVal;

        while (true) {
            origVal = value.get();
            newVal = origVal * multiple;

            if (value.compareAndSet(origVal, newVal))
                return newVal;
        }
    }

    @Override
    public int intValue() {
        return value.get().intValue();
    }

    @Override
    public long longValue() {
        return value.get().longValue();
    }

    @Override
    public float floatValue() {
        return value.get().floatValue();
    }

    @Override
    public double doubleValue() {
        return value.get();
    }
}
