package typestrike.model;

import java.util.Random;
import java.util.Set;

/** คำแรกทำลายเกราะและเปลี่ยนคำ พิมพ์คำใหม่อีกครั้งจึงกำจัดศัตรูได้ */
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
    @Override public boolean restoreArmor() {
        if (armored) return false;
        armored = true;
        return true;
    }
    @Override public int getReward() { return 25; }
    @Override public String getKind() { return "ARMORED"; }
}
