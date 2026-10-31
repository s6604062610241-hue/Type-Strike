package typestrike.model;

import java.util.Random;
import java.util.Set;

public final class NormalEnemy extends Enemy {
    public NormalEnemy(long id, String word, double x, double y) { super(id, word, x, y); }

    @Override public boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords) {
        return true;
    }
    @Override public int getReward() { return 10; }
    @Override public String getKind() { return "NORMAL"; }
}
