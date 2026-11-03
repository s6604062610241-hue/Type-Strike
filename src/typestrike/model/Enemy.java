package typestrike.model;

import java.util.Random;
import java.util.Set;

/** รวมข้อมูลตำแหน่งและการเดินไว้ในคลาสแม่ ให้คลาสลูกกำหนดผลเมื่อคำตรง */
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
        // เริ่มช้าลง 15% ไม่เร่งใน 30 วินาทีแรก จากนั้นเพิ่ม 10% ต่อนาที สูงสุดเพิ่ม 50%
        double progression = 1 + Math.min(0.5, Math.max(0, elapsed - 30) / 600.0);
        return lengthSpeed * 0.85 * progression * speedMultiplier();
    }

    /** คืน true เมื่อการพิมพ์คำตรงครั้งนี้กำจัดศัตรูได้ */
    public abstract boolean onWordMatched(WordRepository words, Random random, Set<String> activeWords);
    public abstract int getReward();
    public abstract String getKind();
    protected double speedMultiplier() { return 1; }
    protected final void setWord(String value) { word = value; }
    public boolean hasArmor() { return false; }
    public boolean dropsSlowCharge() { return false; }
    public double getBlastRadius() { return 0; }
    /** ศัตรูทั่วไปคืนเกราะไม่ได้ คลาสศัตรูเกราะเป็นผู้ดูแลสถานะนี้ */
    public boolean restoreArmor() { return false; }
    public final String getWord() { return word; }
    public final double getX() { return x; }
    public final double getY() { return y; }
    public final long getId() { return id; }
}
