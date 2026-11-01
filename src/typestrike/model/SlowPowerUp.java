package typestrike.model;

/** One stored charge; a timed effect that changes movement, not the game clock. */
public final class SlowPowerUp {
    public static final double DURATION = 4.0;
    public static final double SPEED_FACTOR = 0.5;
    private boolean charged;
    private double remaining;

    public boolean collect() {
        if (charged) return false;
        charged = true;
        return true;
    }

    public boolean activate() {
        if (!charged || isActive()) return false;
        charged = false;
        remaining = DURATION;
        return true;
    }

    /** Split a frame at expiration so its un-slowed portion uses normal speed. */
    public double advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid frame time");
        double affected = Math.min(seconds, remaining);
        remaining = Math.max(0, remaining - seconds);
        return seconds - affected * (1 - SPEED_FACTOR);
    }

    public boolean isCharged() { return charged; }
    public boolean isActive() { return remaining > 0; }
    public double getRemaining() { return remaining; }
}
