package typestrike.model;

import java.util.ArrayList;
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
    private final List<Enemy> enemies = new ArrayList<>();
    private double elapsed;
    private double spawnCountdown = 0.8;
    private int score;
    private int defeated;
    private int submissions;
    private int matches;
    private int spawned;
    private boolean paused;

    public record HitResult(boolean matched, boolean eliminated, double x, double y, int reward) { }

    public GameModel(WordRepository words, Random random) {
        this.words = words;
        this.random = random;
    }

    public void update(double seconds) {
        if (paused || isGameOver() || seconds <= 0) return;
        if (!Double.isFinite(seconds)) throw new IllegalArgumentException("Invalid elapsed time");
        elapsed += seconds;
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            enemy.move(seconds, elapsed, BASE_X, BASE_Y);
            if (Math.hypot(enemy.getX() - BASE_X, enemy.getY() - BASE_Y) <= 36) {
                base.takeDamage(enemy.getWord().length());
                iterator.remove();
                if (isGameOver()) return;
            }
        }
        spawnCountdown -= seconds;
        if (spawnCountdown <= 0) {
            if (enemies.size() < MAX_ENEMIES) spawnEnemy();
            spawnCountdown = Math.max(0.7, 2.2 - elapsed / 120.0);
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
            boolean crowded = false;
            for (Enemy other : enemies) {
                if (Math.hypot(x - other.getX(), y - other.getY()) < 105) crowded = true;
            }
            if (!crowded) break;
            if (attempt == 29) return;
        }
        String word = words.nextWord(random, activeWords());
        spawned++;
        // The sixth enemy introduces armor; later spawns mix both types.
        boolean armor = spawned == 6 || (spawned > 6 && random.nextDouble() < 0.28);
        enemies.add(armor ? new ArmoredEnemy(spawned, word, x, y) : new NormalEnemy(spawned, word, x, y));
    }

    private Set<String> activeWords() {
        Set<String> result = new HashSet<>();
        for (Enemy enemy : enemies) result.add(enemy.getWord());
        return result;
    }

    public HitResult submit(String input) {
        if (paused || isGameOver() || input == null || input.isBlank()) return new HitResult(false, false, 0, 0, 0);
        submissions++;
        String word = input.trim().toLowerCase(Locale.ROOT);
        Iterator<Enemy> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (!enemy.getWord().equals(word)) continue;
            matches++;
            // Polymorphism: each subtype decides how to react to the same call.
            boolean eliminated = enemy.onWordMatched(words, random, activeWords());
            int reward = eliminated ? enemy.getReward() : 0;
            if (eliminated) {
                iterator.remove();
                score += reward;
                defeated++;
            }
            return new HitResult(true, eliminated, enemy.getX(), enemy.getY(), reward);
        }
        return new HitResult(false, false, 0, 0, 0);
    }

    public void togglePause() { if (!isGameOver()) paused = !paused; }
    public boolean isPaused() { return paused; }
    public boolean isGameOver() { return base.isDestroyed(); }
    public Base getBase() { return base; }
    public List<Enemy> getEnemies() { return Collections.unmodifiableList(enemies); }
    public double getElapsed() { return elapsed; }
    public int getScore() { return score; }
    public int getDefeated() { return defeated; }
    public int getAccuracy() { return submissions == 0 ? 100 : (int) Math.round(matches * 100.0 / submissions); }
    public int getLevel() { return 1 + (int) (elapsed / 30); }
}
