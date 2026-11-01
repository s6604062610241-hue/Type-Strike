package typestrike;

import java.util.List;
import java.util.Random;
import typestrike.model.Enemy;
import typestrike.model.GameModel;
import typestrike.model.NormalEnemy;
import typestrike.model.WordRepository;

/** Player-facing pace guarantees rather than a duplicate of the difficulty formula. */
public final class DifficultyBalanceTest {
    public static void main(String[] args) {
        Enemy enemy = new NormalEnemy(1, "cat", 0, 0);
        double start = enemy.getSpeed(0);
        if (enemy.getSpeed(30) != start) throw new AssertionError("First 30 seconds should not accelerate");
        if (enemy.getSpeed(60) > start * 1.1) throw new AssertionError("First minute ramps too fast");
        if (enemy.getSpeed(600) > start * 1.5 + 0.000001) throw new AssertionError("Long-run speed cap too high");
        GameModel game = new GameModel(new WordRepository(), new Random(55));
        int spawned = 0;
        for (int frame = 0; frame < 600; frame++) {
            game.update(0.1);
            for (Enemy target : List.copyOf(game.getEnemies())) {
                spawned++;
                if (!game.submit(target.getWord()).eliminated()) game.submit(target.getWord());
            }
        }
        if (spawned < 15 || spawned > 24) throw new AssertionError("First minute should give a manageable 15-24 enemies, got " + spawned);
        System.out.println("PASS: no opening acceleration, gentle first minute, speed cap; " + spawned + " enemies in first simulated minute.");
    }
}
