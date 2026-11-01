package typestrike.model;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/** Pure game rules. No Swing or filesystem dependencies. Updated by one UI timer. */
public final class GameModel {
    public static final int WIDTH = 1100;
    public static final int HEIGHT = 540;
    public static final double BASE_X = WIDTH / 2.0;
    public static final double BASE_Y = HEIGHT / 2.0;
    public static final int MAX_ENEMIES = 14;
    private final WordRepository words;
    private final Random random;
    // Composition: a game owns a base and a collection of enemies.
    private final Base base = new Base();
    private final SlowPowerUp slow = new SlowPowerUp();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<CombatEffect> combatEffects = new ArrayList<>();
    private double elapsed;
    private double spawnCountdown = 0.8;
    private int score;
    private int defeated;
    private int submissions;
    private int matches;
    private int spawned;
    private boolean paused;

    public record HitResult(boolean matched, boolean eliminated, double x, double y, int reward,
                            boolean frostReward, boolean chargeAdded) { }
    public record CombatEffect(String kind, double x, double y, double targetX, double targetY,
                               double remaining) { }

    public GameModel(WordRepository words, Random random) {
        this.words = words;
        this.random = random;
    }

    public void update(double seconds) {
        if (paused || isGameOver() || seconds <= 0) return;
        if (!Double.isFinite(seconds)) throw new IllegalArgumentException("Invalid elapsed time");
        elapsed += seconds;
        for (int i = 0; i < combatEffects.size(); i++) {
            CombatEffect e = combatEffects.get(i);
            combatEffects.set(i, new CombatEffect(e.kind(), e.x(), e.y(), e.targetX(), e.targetY(), e.remaining() - seconds));
        }
        combatEffects.removeIf(e -> e.remaining() <= 0);
        double movementSeconds = slow.advance(seconds);
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.move(movementSeconds, elapsed, BASE_X, BASE_Y);
            if (Math.hypot(enemy.getX() - BASE_X, enemy.getY() - BASE_Y) <= 36) {
                base.takeDamage(enemy.getWord().length());
                iterator.remove();
                if (isGameOver()) return;
            }
        }
        for (Enemy enemy : enemies) {
            if (enemy instanceof HealerEnemy healer && healer.advanceHealing(seconds)) {
                Enemy target = getHealTarget(healer);
                if (target != null && target.restoreArmor()) {
                    combatEffects.add(new CombatEffect("HEAL", healer.getX(), healer.getY(), target.getX(), target.getY(), 0.7));
                }
            }
        }
        spawnCountdown -= seconds;
        if (spawnCountdown <= 0) {
            if (enemies.size() < MAX_ENEMIES) spawnEnemy();
            spawnCountdown = Math.max(1.2, 2.8 - Math.max(0, elapsed - 30) / 180.0);
        }
    }

    private void spawnEnemy() {
        double x = 0;
        double y = 0;
        // Avoid stacked labels at the spawn point; moving paths may still cross.
        for (int attempt = 0; attempt < 30; attempt++) {
            int side = random.nextInt(4);
            x = side == 0 ? 70 : side == 1 ? WIDTH - 70 : 70 + random.nextDouble() * (WIDTH - 140);
            y = side == 2 ? 64 : side == 3 ? HEIGHT - 45 : 64 + random.nextDouble() * (HEIGHT - 109);
            boolean crowded = y < 100 && x > 825; // Keep sprites clear of the health HUD.
            for (Enemy other : enemies) {
                if (Math.hypot(x - other.getX(), y - other.getY()) < 105) crowded = true;
            }
            if (!crowded) break;
            if (attempt == 29) return;
        }
        String word = words.nextWord(random, activeWords());
        spawned++;
        // Guaranteed introductions let the player learn each rule before random mixes.
        double roll = random.nextDouble();
        if (spawned == 12) {
            enemies.add(new BlastEnemy(spawned, word, x, y));
        } else if (spawned == 16 && enemies.stream().noneMatch(e -> e instanceof HealerEnemy)) {
            enemies.add(new HealerEnemy(spawned, word, x, y));
        } else if (spawned == 8 || (spawned > 8 && roll < 0.16)) {
            enemies.add(new FrostEnemy(spawned, word, x, y));
        } else if (spawned == 6 || (spawned > 6 && roll < (spawned > 8 ? 0.44 : 0.28))) {
            enemies.add(new ArmoredEnemy(spawned, word, x, y));
        } else if (spawned == 12 || (spawned > 12 && roll < 0.56)) {
            enemies.add(new BlastEnemy(spawned, word, x, y));
        } else if ((spawned == 16 || (spawned > 16 && roll < 0.64))
                && enemies.stream().noneMatch(e -> e instanceof HealerEnemy)) {
            enemies.add(new HealerEnemy(spawned, word, x, y));
        } else {
            enemies.add(new NormalEnemy(spawned, word, x, y));
        }
    }

    private Set<String> activeWords() {
        Set<String> result = new HashSet<>();
        for (Enemy enemy : enemies) result.add(enemy.getWord());
        return result;
    }

    public HitResult submit(String input) {
        if (paused || isGameOver() || input == null || input.isBlank()) return noHit();
        submissions++;
        String word = input.trim().toLowerCase(Locale.ROOT);
        for (Enemy enemy : List.copyOf(enemies)) {
            if (!enemy.getWord().equals(word)) continue;
            matches++;
            // Polymorphism: each subtype decides how to react to the same call.
            boolean eliminated = enemy.onWordMatched(words, random, activeWords());
            int beforeScore = score;
            int beforeCharges = slow.getCharges();
            boolean frostReward = false;
            if (eliminated) {
                ArrayDeque<Enemy> blasts = new ArrayDeque<>();
                frostReward = eliminate(enemy, blasts);
                while (!blasts.isEmpty()) {
                    Enemy blast = blasts.removeFirst();
                    combatEffects.add(new CombatEffect("BLAST", blast.getX(), blast.getY(), 0, 0, 0.7));
                    for (Enemy nearby : List.copyOf(enemies)) {
                        if (Math.hypot(nearby.getX() - blast.getX(), nearby.getY() - blast.getY()) <= blast.getBlastRadius()
                                && nearby.onWordMatched(words, random, activeWords())) {
                            frostReward |= eliminate(nearby, blasts);
                        }
                    }
                }
            }
            return new HitResult(true, eliminated, enemy.getX(), enemy.getY(), score - beforeScore,
                    frostReward, slow.getCharges() > beforeCharges);
        }
        return noHit();
    }

    private boolean eliminate(Enemy enemy, ArrayDeque<Enemy> blasts) {
        if (!enemies.remove(enemy)) return false;
        score += enemy.getReward();
        defeated++;
        if (enemy.getBlastRadius() > 0) blasts.addLast(enemy);
        if (enemy.dropsSlowCharge()) { slow.collect(); return true; }
        return false;
    }

    /** Nearest exposed armored enemy; keeping its word preserves any typing in progress. */
    public Enemy getHealTarget(HealerEnemy healer) {
        if (!enemies.contains(healer)) return null;
        Enemy target = null;
        double distance = HealerEnemy.RANGE;
        for (Enemy enemy : enemies) {
            if (!(enemy instanceof ArmoredEnemy) || enemy.hasArmor()) continue;
            double d = Math.hypot(enemy.getX() - healer.getX(), enemy.getY() - healer.getY());
            if (d <= distance && (target == null || d < distance || enemy.getId() < target.getId())) {
                distance = d;
                target = enemy;
            }
        }
        return target;
    }

    public List<CombatEffect> getCombatEffects() { return Collections.unmodifiableList(combatEffects); }

    private HitResult noHit() { return new HitResult(false, false, 0, 0, 0, false, false); }

    public boolean activateSlow() {
        return !paused && !isGameOver() && slow.activate();
    }

    public boolean hasSlowCharge() { return slow.isCharged(); }
    public int getSlowCharges() { return slow.getCharges(); }
    public boolean isSlowActive() { return slow.isActive(); }
    public double getSlowRemaining() { return slow.getRemaining(); }

    public void togglePause() { if (!isGameOver()) paused = !paused; }
    public boolean isPaused() { return paused; }
    public boolean isGameOver() { return base.isDestroyed(); }
    public Base getBase() { return base; }
    public List<Enemy> getEnemies() { return Collections.unmodifiableList(enemies); }
    public double getElapsed() { return elapsed; }
    public int getScore() { return score; }
    public int getDefeated() { return defeated; }
    public int getAccuracy() { return submissions == 0 ? 100 : (int) Math.round(matches * 100.0 / submissions); }
    public int getLevel() { return 1 + (int) (elapsed / 60); }
}
