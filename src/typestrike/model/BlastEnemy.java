package typestrike.model;

import java.util.Random;
import java.util.Set;

/** One word detonates a friendly-to-the-tower blast, including chain reactions. */
public final class BlastEnemy extends Enemy {
    public static final double RADIUS = 140;
    public BlastEnemy(long id, String word, double x, double y) { super(id, word, x, y); }
    @Override public boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords) { return true; }
    @Override public int getReward() { return 15; }
    @Override public String getKind() { return "BLAST"; }
    @Override protected double speedMultiplier() { return 0.85; }
    @Override public double getBlastRadius() { return RADIUS; }
}
