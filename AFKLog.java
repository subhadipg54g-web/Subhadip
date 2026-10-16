package com.example.afkprogression.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/** Clean, rate-limited logger. Lifecycle messages always show; debug messages need the config flag. */
public final class AFKLog {
    private static final Logger LOG = LoggerFactory.getLogger("AFKProgression");
    private static final Map<String, String> LAST = new HashMap<>();
    private static volatile boolean verbose;
    private static volatile java.util.function.LongSupplier clock = () -> -1L;

    private AFKLog() {}

    public static void setVerbose(boolean v) { verbose = v; }
    public static boolean verbose() { return verbose; }
    /** Lets verbose logs carry the game tick, which makes timing problems far easier to read. */
    public static void setClock(java.util.function.LongSupplier c) { clock = c == null ? () -> -1L : c; }
    private static String stamp() { return verbose ? "[t=" + clock.getAsLong() + "] " : ""; }

    public static void info(String msg) { LOG.info("[AFK] {}{}", stamp(), msg); }
    public static void warn(String msg) { LOG.warn("[AFK] {}", msg); }
    public static void warn(String msg, Throwable t) { LOG.warn("[AFK] " + msg, t); }
    public static void error(String msg, Throwable t) { LOG.error("[AFK] " + msg, t); }

    public static void debug(String msg) {
        if (verbose) LOG.info("[AFK] {}{}", stamp(), msg);
    }

    /** Logs only when the message for {@code key} changes, so state machines stay quiet between transitions. */
    public static void change(String key, String msg) {
        if (!verbose) return;
        String prev = LAST.put(key, msg);
        if (!msg.equals(prev)) LOG.info("[AFK] {}{}", stamp(), msg);
    }
}
