package typestrike.model;

import java.util.Random;
import java.util.Set;

/** Abstraction and inheritance: shared position/movement, specialized hit behavior. */
public abstract class Enemy {
    private double x;
    private double y;
    private String word;
    private final long id;

    protected Enemy(long id, String word, double x, double y) {
        this.id = id;
        this.word = word;
        this.x = x;
        this.y = y;
    }

    public final void move(double seconds, double elapsed, double targetX, double targetY) {
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);
        double travel = Math.min(distance, getSpeed(elapsed) * seconds);
        if (distance > 0) {
            x += dx / distance * travel;
            y += dy / distance * travel;
        }
    }

    public final double getSpeed(double elapsed) {
        double lengthSpeed = Math.max(22, Math.min(56, 190.0 / (word.length() + 1)));
        return lengthSpeed * (1 + Math.min(0.8, elapsed / 180.0)) * speedMultiplier();
    }

    /** Returns true when this matching word eliminates the enemy. */
    public abstract boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords);
    public abstract int getReward();
    public abstract String getKind();
    protected double speedMultiplier() { return 1; }
    protected final void setWord(String value) { word = value; }
    public boolean hasArmor() { return false; }
    public boolean dropsSlowCharge() { return false; }
    public final String getWord() { return word; }
    public final double getX() { return x; }
    public final double getY() { return y; }
    public final long getId() { return id; }
}
