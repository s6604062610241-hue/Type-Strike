package typestrike;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Set;
import typestrike.model.*;

/** จัดสถานการณ์ศัตรูเพื่อทดสอบผลร่วมกัน โดยไม่ต้องรอให้สุ่มเกิด */
public final class SpecialEnemiesTest {
    private static int checks;
    private static final WordRepository WORDS = new WordRepository();
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    @SuppressWarnings("unchecked")
    static GameModel encounter(Enemy... enemies) throws Exception {
        GameModel game = new GameModel(WORDS, new Random(19));
        Field field = GameModel.class.getDeclaredField("enemies"); field.setAccessible(true);
        ((List<Enemy>) field.get(game)).addAll(List.of(enemies));
        Field countdown = GameModel.class.getDeclaredField("spawnCountdown"); countdown.setAccessible(true);
        countdown.setDouble(game, 10000);
        return game;
    }
    private static ArmoredEnemy exposed(long id, String word, double x, double y) {
        ArmoredEnemy enemy = new ArmoredEnemy(id, word, x, y);
        enemy.onWordMatched(WORDS, new Random((int) id), Set.of(word));
        return enemy;
    }
    public static void main(String[] args) throws Exception {
        ArmoredEnemy armor = new ArmoredEnemy(2, "shield", 150, 100);
        Enemy normal = new NormalEnemy(3, "cat", 100, 120);
        Enemy far = new NormalEnemy(4, "dog", 500, 100);
        GameModel blast = encounter(new BlastEnemy(1, "blast", 100, 100), armor, normal, far);
        GameModel.HitResult hit = blast.submit("blast");
        check(hit.matched() && hit.eliminated() && hit.reward() == 25, "Blast scores primary and nearby kill");
        check(blast.getDefeated() == 2 && !armor.hasArmor(), "One explosion breaks only one armor layer");
        check(blast.getEnemies().contains(far) && blast.getEnemies().contains(armor), "Out-of-range and armored enemies survive");
        check(blast.getBase().getHealth() == 100 && blast.getAccuracy() == 100, "No base damage or artificial typing attempts");
        check(!blast.submit("blast").matched() && blast.getScore() == 25, "Cannot score twice");
        check(blast.submit(armor.getWord()).eliminated() && blast.getScore() == 50, "Finish armor after explosion");

        GameModel chain = encounter(new BlastEnemy(1, "blast", 100, 100),
                new BlastEnemy(2, "spark", 230, 100), new FrostEnemy(3, "ice", 360, 100),
                new NormalEnemy(4, "tree", 500, 100));
        hit = chain.submit("blast");
        check(hit.reward() == 50 && hit.frostReward() && hit.chargeAdded(), "Chain blast carries Frost reward");
        check(chain.getSlowCharges() == 1 && chain.getDefeated() == 3 && chain.getEnemies().size() == 1, "Chains stop at blast radius");
        check(chain.getCombatEffects().size() == 2, "Both explosion positions are visible");
        chain.togglePause(); chain.update(10);
        check(chain.getCombatEffects().get(0).remaining() == 0.7, "Pause freezes effect lifetime");
        chain.togglePause(); chain.update(0.8);
        check(chain.getCombatEffects().isEmpty(), "Effects expire");

        ArmoredEnemy twice = new ArmoredEnemy(3, "armor", 160, 100);
        GameModel overlap = encounter(new BlastEnemy(1, "blast", 100, 100), new BlastEnemy(2, "spark", 200, 100), twice);
        overlap.submit("blast");
        check(overlap.getEnemies().isEmpty() && overlap.getScore() == 55, "Two separate blasts can break then kill armor once");
        GameModel contact = encounter(new BlastEnemy(1, "blast", GameModel.BASE_X, GameModel.BASE_Y),
                new NormalEnemy(2, "cat", GameModel.BASE_X + 90, GameModel.BASE_Y));
        contact.update(0.01);
        check(contact.getBase().getHealth() == 95 && contact.getEnemies().size() == 1 && contact.getCombatEffects().isEmpty(),
                "Base collision causes ordinary word damage, never a blast");

        HealerEnemy healer = new HealerEnemy(1, "healer", 50, 270);
        ArmoredEnemy near = exposed(2, "shield", 80, 270);
        ArmoredEnemy further = exposed(3, "armor", 180, 270);
        String word = near.getWord();
        GameModel healing = encounter(healer, near, further, new NormalEnemy(4, "cat", 70, 270));
        check(healing.getHealTarget(healer) == near, "Healer targets nearest exposed armor, not normal enemy");
        healing.update(6.6);
        check(healer.isPreparingHeal() && !near.hasArmor(), "Warning precedes first eight-second heal");
        double remaining = healer.getHealRemaining();
        healing.togglePause(); healing.update(20);
        check(healer.getHealRemaining() == remaining && !near.hasArmor(), "Pause freezes healing");
        healing.togglePause(); healing.update(1.4);
        check(near.hasArmor() && !further.hasArmor() && near.getWord().equals(word), "Heal restores one layer and preserves typed word");
        check(!near.restoreArmor() && !new NormalEnemy(10, "dog", 0, 0).restoreArmor(), "No stacked armor or new armor on normals");
        check(healing.getCombatEffects().stream().anyMatch(e -> e.kind().equals("HEAL")), "Successful heal has feedback");
        check(healing.getScore() == 0 && healing.getDefeated() == 0, "Healing never grants points");
        healing.submit(word);
        check(!near.hasArmor() && !near.getWord().equals(word), "Restored armor again needs a new word after breaking");
        check(healing.submit(near.getWord()).eliminated(), "Player can finish restored enemy normally");

        HealerEnemy cancelled = new HealerEnemy(1, "healer", 50, 270);
        ArmoredEnemy patient = exposed(2, "armor", 100, 270);
        GameModel stopped = encounter(cancelled, patient);
        stopped.update(6.6); stopped.submit("healer"); stopped.update(2);
        check(!patient.hasArmor() && stopped.getScore() == 20, "Killing healer cancels pending heal");
        HealerEnemy alone = new HealerEnemy(1, "healer", 50, 270);
        GameModel lostTarget = encounter(alone, exposed(2, "armor", 100, 270));
        lostTarget.update(6.6); lostTarget.submit(lostTarget.getEnemies().get(1).getWord()); lostTarget.update(1.4);
        check(lostTarget.getHealTarget(alone) == null && lostTarget.getCombatEffects().isEmpty(), "Dead target cannot be resurrected");
        HealerEnemy remote = new HealerEnemy(1, "healer", 50, 270);
        GameModel outside = encounter(remote, exposed(2, "armor", 1000, 500));
        check(outside.getHealTarget(remote) == null, "Healer respects range");

        GameModel spawns = new GameModel(WORDS, new Random(77));
        boolean seenBlast = false, seenHealer = false;
        for (int i = 0; i < 12000; i++) {
            spawns.update(0.1);
            check(spawns.getEnemies().stream().filter(e -> e instanceof HealerEnemy).count() <= 1, "One healer maximum");
            for (Enemy enemy : List.copyOf(spawns.getEnemies())) {
                if (!spawns.getEnemies().contains(enemy)) continue;
                seenBlast |= enemy instanceof BlastEnemy;
                seenHealer |= enemy instanceof HealerEnemy;
                spawns.submit(enemy.getWord());
                if (spawns.getEnemies().contains(enemy)) spawns.submit(enemy.getWord());
            }
        }
        check(seenBlast && seenHealer && !spawns.isGameOver(), "New types spawn and a perfect player survives twenty minutes");
        System.out.println("PASS: " + checks + " special-enemy checks; blast chains, armor, Frost, healing, cancellation, pause and spawn cap.");
    }
}
