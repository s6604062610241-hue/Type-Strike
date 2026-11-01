package typestrike.model;

/** Limited stored charges; a timed effect that changes movement, not the game clock. */
public final class SlowPowerUp {
    public static final double DURATION = 5.0;
    public static final int MAX_CHARGES = 3;
    public static final double SPEED_FACTOR = 0.5;
    private int charges;
    private double remaining;

    public boolean collect() {
        if (charges >= MAX_CHARGES) return false;
        charges++;
        return true;
    }

    public boolean activate() {
        if (charges == 0 || isActive()) return false;
        charges--;
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

    public boolean isCharged() { return charges > 0; }
    public int getCharges() { return charges; }
    public boolean isActive() { return remaining > 0; }
    public double getRemaining() { return remaining; }
}
