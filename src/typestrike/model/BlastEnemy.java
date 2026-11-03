package typestrike.model;

import java.util.Random;
import java.util.Set;

/** พิมพ์คำตรงแล้วระเบิดใส่ศัตรูใกล้เคียง ระเบิดต่อกันได้และไม่ทำร้ายฐาน */
public final class BlastEnemy extends Enemy {
    public static final double RADIUS = 140;
    public BlastEnemy(long id, String word, double x, double y) { super(id, word, x, y); }
    @Override public boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords) { return true; }
    @Override public int getReward() { return 15; }
    @Override public String getKind() { return "BLAST"; }
    @Override protected double speedMultiplier() { return 0.85; }
    @Override public double getBlastRadius() { return RADIUS; }
}
