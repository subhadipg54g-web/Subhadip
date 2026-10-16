package com.example.afkprogression.util;

import java.util.Locale;

public final class Mathx {
    private Mathx() {}

    public static float wrapDegrees(float d) {
        d %= 360f;
        if (d >= 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
    public static int clamp(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }
    public static float clamp(float v, float lo, float hi) { return v < lo ? lo : Math.min(v, hi); }

    public static String clock(long ticks) {
        long s = ticks / 20;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    /** "iron_ore" -> "Iron Ore". */
    public static String nice(String id) {
        int c = id.indexOf(':');
        String s = c >= 0 ? id.substring(c + 1) : id;
        StringBuilder b = new StringBuilder();
        for (String part : s.split("_")) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    public static String grouped(long n) { return String.format(Locale.ROOT, "%,d", n); }
}
