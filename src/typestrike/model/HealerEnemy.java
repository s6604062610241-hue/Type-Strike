package typestrike.model;

import java.util.Random;
import java.util.Set;

/** Support clock is encapsulated here; the model selects a living nearby target. */
public final class HealerEnemy extends Enemy {
    public static final double RANGE = 190;
    public static final double INTERVAL = 8;
    public static final double WARNING = 1.5;
    private double remaining = INTERVAL;
    public HealerEnemy(long id, String word, double x, double y) { super(id, word, x, y); }
    public boolean advanceHealing(double seconds) {
        remaining -= seconds;
        if (remaining > 1e-9) return false;
        remaining = INTERVAL; // One heal per update; never burst-heal after a stalled frame.
        return true;
    }
    public boolean isPreparingHeal() { return remaining <= WARNING; }
    public double getHealRemaining() { return remaining; }
    @Override public boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords) { return true; }
    @Override public int getReward() { return 20; }
    @Override public String getKind() { return "HEALER"; }
    @Override protected double speedMultiplier() { return 0.7; }
}
