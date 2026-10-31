package typestrike.model;

import java.util.Random;
import java.util.Set;

/** A first hit breaks armor and assigns a new word; the second eliminates it. */
public final class ArmoredEnemy extends Enemy {
    private boolean armored = true;

    public ArmoredEnemy(long id, String word, double x, double y) { super(id, word, x, y); }

    @Override public boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords) {
        if (!armored) return true;
        String replacement = words.nextWord(random, activeWords);
        setWord(replacement);
        armored = false;
        return false;
    }
    @Override protected double speedMultiplier() { return 0.8; }
    @Override public boolean hasArmor() { return armored; }
    @Override public int getReward() { return 25; }
    @Override public String getKind() { return "ARMORED"; }
}
