package typestrike;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import typestrike.model.ArmoredEnemy;
import typestrike.model.Base;
import typestrike.model.Enemy;
import typestrike.model.GameModel;
import typestrike.model.NormalEnemy;
import typestrike.model.WordRepository;
import typestrike.storage.HighScoreStore;

/** Deterministic integration tests; no extra testing libraries required. */
public final class GameRulesTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        WordRepository words = new WordRepository();
        check(words.size() >= 200, "Vocabulary should be bundled and varied");
        Base base = new Base();
        base.takeDamage(4);
        check(base.getHealth() == 96, "Damage follows word length");
        base.takeDamage(1000);
        check(base.getHealth() == 0 && base.isDestroyed(), "HP must clamp to zero");
        try { base.takeDamage(-1); throw new AssertionError("Negative damage accepted"); }
        catch (IllegalArgumentException expected) { checks++; }

        Enemy shortWord = new NormalEnemy(1, "cat", 10, 10);
        Enemy longWord = new NormalEnemy(2, "mountain", 10, 10);
        check(shortWord.getSpeed(0) > longWord.getSpeed(0), "Short words should move faster");
        check(shortWord.getSpeed(100) > shortWord.getSpeed(0), "Speed should rise over time");
        check(shortWord.getSpeed(1000) == shortWord.getSpeed(5000), "Speed increase should cap");
        check(shortWord.onWordMatched(words, new Random(1), Set.of("cat")), "Normal enemy requires one word");
        ArmoredEnemy armor = new ArmoredEnemy(3, "shield", 0, 0);
        check(!armor.onWordMatched(words, new Random(2), Set.of("shield", "cat")), "First hit must not kill armor");
        check(!armor.hasArmor() && !Set.of("shield", "cat").contains(armor.getWord()), "Armor assigns a unique new word");
        check(armor.onWordMatched(words, new Random(3), Set.of(armor.getWord())), "Second hit eliminates armor");

        GameModel game = new GameModel(words, new Random(42));
        game.update(0.8);
        check(game.getEnemies().size() == 1, "First enemy should spawn");
        Enemy target = game.getEnemies().get(0);
        String targetWord = target.getWord();
        double x = target.getX();
        double elapsed = game.getElapsed();
        game.togglePause();
        game.update(10);
        check(game.getElapsed() == elapsed && target.getX() == x, "Pause freezes time and movement");
        check(!game.submit(targetWord).matched() && game.getScore() == 0, "Cannot attack while paused");
        game.togglePause();
        check(!game.submit("not-a-real-word").matched() && game.getBase().getHealth() == 100, "Mistype does not damage base");
        check(game.submit("  " + targetWord.toUpperCase(java.util.Locale.ROOT) + "  ").eliminated(), "Normalize case and spaces");
        check(game.getScore() == 10 && game.getDefeated() == 1 && game.getAccuracy() == 50, "Score and match rate");
        check(!game.submit(targetWord).matched() && game.getScore() == 10, "A kill cannot score twice");

        GameModel bot = new GameModel(words, new Random(17));
        int armorBreaks = 0;
        int expectedScore = 0;
        for (int frame = 0; frame < 6000; frame++) {
            bot.update(0.1);
            check(bot.getEnemies().size() <= GameModel.MAX_ENEMIES, "Spawn cap");
            Set<String> active = new HashSet<>();
            for (Enemy enemy : List.copyOf(bot.getEnemies())) {
                check(active.add(enemy.getWord()), "No duplicate visible words");
                int reward = enemy.getReward();
                GameModel.HitResult hit = bot.submit(enemy.getWord());
                if (!hit.eliminated()) {
                    armorBreaks++;
                    check(bot.submit(enemy.getWord()).eliminated(), "Two hits eliminate armored enemy in model");
                }
                expectedScore += reward;
            }
        }
        check(armorBreaks > 10 && bot.getScore() == expectedScore, "Mixed enemy scoring over ten simulated minutes");
        check(!bot.isGameOver(), "Perfect bot survives");

        GameModel idle = new GameModel(words, new Random(23));
        for (int frame = 0; frame < 20000 && !idle.isGameOver(); frame++) {
            idle.update(0.1);
            check(idle.getEnemies().size() <= GameModel.MAX_ENEMIES, "Idle spawn cap");
            Set<String> active = new HashSet<>();
            for (Enemy enemy : idle.getEnemies()) check(active.add(enemy.getWord()), "Unique words under crowding");
        }
        check(idle.isGameOver() && idle.getBase().getHealth() == 0, "Idle game eventually ends");
        double endTime = idle.getElapsed();
        idle.update(50);
        check(idle.getElapsed() == endTime, "Game over freezes rules");
        GameModel fresh = new GameModel(words, new Random(24));
        check(fresh.getScore() == 0 && fresh.getBase().getHealth() == 100 && fresh.getElapsed() == 0, "Replay is a fresh model");

        Path folder = Files.createTempDirectory(Path.of("build"), "score-test-");
        Path scoreFile = folder.resolve("scores.txt");
        HighScoreStore store = new HighScoreStore(scoreFile);
        for (int score : new int[]{10, 300, 20, 100, 100, 0}) check(store.record(score), "Can save score");
        check(store.getScores().equals(List.of(300, 100, 100)), "Top three sorted, tied runs allowed");
        check(new HighScoreStore(scoreFile).getScores().equals(store.getScores()), "Scores persist between sessions");
        Files.writeString(scoreFile, "junk\n-10\n42\n2147483649999\n");
        check(new HighScoreStore(scoreFile).getScores().equals(List.of(42)), "Damaged score entries are ignored");
        Path blocked = folder.resolve("not-a-directory");
        Files.writeString(blocked, "test");
        HighScoreStore failed = new HighScoreStore(blocked.resolve("scores.txt"));
        check(!failed.record(25) && failed.getBest() == 25 && !failed.getWarning().isBlank(), "Unwritable save preserves session and reports warning");
        System.out.println("PASS: " + checks + " checks; gameplay, armor, pause, game over, ten-minute simulation, persistence.");
    }
}
