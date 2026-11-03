package typestrike.model;

import java.util.Random;
import java.util.Set;

/** คลาสนี้เก็บเวลารักษา ส่วน GameModel เลือกศัตรูใกล้เคียงที่ยังอยู่บนสนาม */
public final class HealerEnemy extends Enemy {
    public static final double RANGE = 190;
    public static final double INTERVAL = 8;
    public static final double WARNING = 1.5;
    private double remaining = INTERVAL;
    public HealerEnemy(long id, String word, double x, double y) { super(id, word, x, y); }
    public boolean advanceHealing(double seconds) {
        remaining -= seconds;
        if (remaining > 1e-9) return false;
        remaining = INTERVAL; // รักษาครั้งเดียวต่อการอัปเดต ไม่รักษารัวชดเชยเมื่อหน้าจอค้าง
        return true;
    }
    public boolean isPreparingHeal() { return remaining <= WARNING; }
    public double getHealRemaining() { return remaining; }
    @Override public boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords) { return true; }
    @Override public int getReward() { return 20; }
    @Override public String getKind() { return "HEALER"; }
    @Override protected double speedMultiplier() { return 0.7; }
}
