package com.example.afkprogression.util;

/** Tick-based rate limiter. */
public final class Throttle {
    private final int interval;
    private long next;

    public Throttle(int interval) { this.interval = Math.max(1, interval); }

    public boolean ready(long now) {
        if (now < next) return false;
        next = now + interval;
        return true;
    }
    public void reset() { next = 0; }
    public void delay(long now, int ticks) { next = now + ticks; }
}
