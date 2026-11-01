package typestrike.model;

import java.util.Random;
import java.util.Set;

/** Crystal-marked enemy: one word grants a Slow charge as well as points. */
public final class FrostEnemy extends Enemy {
    public FrostEnemy(long id, String word, double x, double y) { super(id, word, x, y); }

    @Override public boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords) {
        return true;
    }
    @Override public int getReward() { return 20; }
    @Override public String getKind() { return "FROST"; }
    @Override public boolean dropsSlowCharge() { return true; }
}
