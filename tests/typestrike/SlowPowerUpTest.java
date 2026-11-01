package typestrike;

import java.util.List;
import java.util.Random;
import typestrike.model.Enemy;
import typestrike.model.GameModel;
import typestrike.model.SlowPowerUp;
import typestrike.model.WordRepository;

public final class SlowPowerUpTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static boolean near(double a, double b) { return Math.abs(a - b) < 0.000001; }

    static GameModel chargedGame() {
        GameModel game = new GameModel(new WordRepository(), new Random(88));
        for (int frame = 0; frame < 1000; frame++) {
            game.update(0.1);
            for (Enemy enemy : List.copyOf(game.getEnemies())) {
                GameModel.HitResult hit = game.submit(enemy.getWord());
                if (!hit.eliminated()) game.submit(enemy.getWord());
                if (hit.frostReward()) return game;
            }
        }
        throw new AssertionError("Guaranteed Frost introduction did not appear");
    }

    public static void main(String[] args) {
        SlowPowerUp power = new SlowPowerUp();
        check(!power.activate() && !power.isCharged(), "Cannot activate without a charge");
        check(power.collect() && !power.collect(), "Store at most one charge");
        check(power.activate() && !power.isCharged(), "Activation consumes charge");
        check(near(power.advance(2), 1) && near(power.getRemaining(), 2), "Half speed for two seconds");
        check(power.collect(), "Can store the next charge while active");
        check(!power.activate() && power.isCharged(), "Cannot stack or spend a banked charge while active");
        check(near(power.advance(3), 2) && !power.isActive(), "Expiration splits slow and normal portions of a frame");
        check(power.activate() && near(power.getRemaining(), 4), "Banked charge works after expiration");
        check(near(power.advance(4), 2) && !power.isActive(), "Exact four-second duration");
        check(near(power.advance(1), 1), "Normal movement after effect expires");

        GameModel slow = chargedGame();
        GameModel normal = chargedGame();
        check(slow.hasSlowCharge() && slow.getScore() >= 20, "Typing a Frost word grants score and charge");
        check(slow.getAccuracy() == 100, "Collecting a power-up does not count as a mistype");
        slow.togglePause();
        check(!slow.activateSlow() && slow.hasSlowCharge(), "Cannot consume charge while paused");
        slow.togglePause();
        for (int i = 0; i < 100 && normal.getEnemies().isEmpty(); i++) {
            slow.update(0.1);
            normal.update(0.1);
        }
        Enemy slowedEnemy = slow.getEnemies().get(0);
        Enemy normalEnemy = normal.getEnemies().get(0);
        check(slowedEnemy.getWord().equals(normalEnemy.getWord()), "Control games have the same target");
        double sx = slowedEnemy.getX(), sy = slowedEnemy.getY();
        double nx = normalEnemy.getX(), ny = normalEnemy.getY();
        check(slow.activateSlow() && !slow.hasSlowCharge(), "Model activates Slow");
        slow.update(0.25);
        normal.update(0.25);
        double slowDistance = Math.hypot(slowedEnemy.getX() - sx, slowedEnemy.getY() - sy);
        double normalDistance = Math.hypot(normalEnemy.getX() - nx, normalEnemy.getY() - ny);
        check(near(slowDistance * 2, normalDistance), "Enemy movement is exactly 50 percent of control");
        check(near(slow.getElapsed(), normal.getElapsed()), "Game clock is not slowed");
        double remaining = slow.getSlowRemaining();
        double elapsed = slow.getElapsed();
        sx = slowedEnemy.getX(); sy = slowedEnemy.getY();
        slow.togglePause();
        slow.update(20);
        check(near(remaining, slow.getSlowRemaining()) && near(elapsed, slow.getElapsed()), "Pause freezes effect and game clocks");
        check(near(sx, slowedEnemy.getX()) && near(sy, slowedEnemy.getY()), "Pause also freezes slowed movement");
        slow.togglePause();
        slow.update(remaining);
        check(!slow.isSlowActive(), "Effect expires after remaining play time");

        // A spawned enemy must inherit the global Slow, not just enemies present at activation.
        GameModel freshSlow = chargedGame();
        GameModel freshNormal = chargedGame();
        freshSlow.activateSlow();
        for (int i = 0; i < 100 && freshNormal.getEnemies().isEmpty(); i++) {
            freshSlow.update(0.1);
            freshNormal.update(0.1);
        }
        Enemy a = freshSlow.getEnemies().get(0), b = freshNormal.getEnemies().get(0);
        sx = a.getX(); sy = a.getY(); nx = b.getX(); ny = b.getY();
        freshSlow.update(0.1); freshNormal.update(0.1);
        check(near(Math.hypot(a.getX() - sx, a.getY() - sy) * 2,
                Math.hypot(b.getX() - nx, b.getY() - ny)), "New spawns are slowed too");
        check(freshSlow.getEnemies().size() == freshNormal.getEnemies().size(), "Spawn schedule is unchanged");

        GameModel idle = new GameModel(new WordRepository(), new Random(10));
        for (int i = 0; i < 2000 && !idle.isGameOver(); i++) idle.update(0.1);
        check(idle.isGameOver() && !idle.hasSlowCharge(), "Frost reaching the base does not grant a charge");
        GameModel ending = chargedGame();
        for (int i = 0; i < 2000 && !ending.isGameOver(); i++) ending.update(0.1);
        check(!ending.activateSlow() && ending.hasSlowCharge(), "Game Over cannot consume a stored charge");
        GameModel replay = new GameModel(new WordRepository(), new Random(11));
        check(!replay.hasSlowCharge() && !replay.isSlowActive(), "Replay starts without carried charges");
        System.out.println("PASS: " + checks + " Slow checks; charge cap, movement, expiration, spawning, pause, game over and replay.");
    }
}
