package com.codessay.concurrency.primitives.structure;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicDouble extends Number {
    private AtomicReference<Double> value;

    public AtomicDouble() {
        this(0.0);
    }

    public AtomicDouble(double initialValue) {
        value = new AtomicReference<>(initialValue);
    }

    public double get() {
        return value.get();
    }

    public void set(double newVal) {
        value.set(newVal);
    }

    public boolean compareAndSet(double expect, double update) {
        Double originalValue, newValue;
        newValue = update;

        while (true) {
            originalValue = value.get();

            if (Double.compare(originalValue, expect) == 0) {
                if (value.compareAndSet(originalValue, newValue))
                    return true;
            } else {
                return false;
            }
        }
    }

    public double getAndSet(double setVal) {
        Double originalValue, newValue;
        newValue = setVal;

        while (true) {
            originalValue = value.get();

            if (value.compareAndSet(originalValue, newValue))
                return originalValue;
        }
    }

    public double getAndAdd(double delta) {
        Double originalValue, newValue;

        while (true) {
            originalValue = value.get();
            newValue = originalValue + delta;

            if (value.compareAndSet(originalValue, newValue))
                return originalValue;
        }
    }

    public double addAndGet(double delta) {
        Double originalValue, newValue;

        while (true) {
            originalValue = value.get();
            newValue = originalValue + delta;

            if (value.compareAndSet(originalValue, newValue))
                return newValue;
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
        Double originalValue, newValue;

        while (true) {
            originalValue = value.get();
            newValue = originalValue * multiple;

            if (value.compareAndSet(originalValue, newValue))
                return originalValue;
        }
    }

    public double multiplyAndGet(double multiple) {
        Double originalValue, newValue;

        while (true) {
            originalValue = value.get();
            newValue = originalValue * multiple;

            if (value.compareAndSet(originalValue, newValue))
                return newValue;
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
