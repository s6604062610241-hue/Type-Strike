package typestrike;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import typestrike.audio.MusicPlayer;
import typestrike.audio.SoundEffectPlayer;
import typestrike.audio.SoundEffectPlayer.Effect;
import typestrike.model.*;
import typestrike.ui.GameWindow;
import typestrike.ui.PlayScreen;

/** ทดสอบตัวรับเหตุการณ์ของหน้าจอจริงกับสถานการณ์ที่กำหนด โดยไม่ต้องมีอุปกรณ์เสียง */
public final class SoundEffectsUiTest {
    private static JButton button(Container root, String prefix) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton b && b.getText().startsWith(prefix)) return b;
            if (child instanceof Container c) { var found = button(c, prefix); if (found != null) return found; }
        }
        return null;
    }
    private static JTextField input(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTextField f) return f;
            if (child instanceof Container c) { var found = input(c); if (found != null) return found; }
        }
        return null;
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void send(PlayScreen play, String text) { var field = input(play); field.setText(text); field.postActionEvent(); }
    private static void expect(List<Effect> actual, Effect... expected) {
        check(actual.equals(List.of(expected)), "Expected " + List.of(expected) + ", got " + actual);
        actual.clear();
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build/ui-review"));
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            var heard = new ArrayList<Effect>();
            var sfx = new SoundEffectPlayer(effect -> new SoundEffectPlayer.Playback() {
                public void play() { heard.add(effect); }
                public void stop() { }
                public void close() { }
            });
            var music = new MusicPlayer(track -> new MusicPlayer.Playback() {
                public void startLoop() { }
                public void stop() { }
                public void close() { }
            });
            GameWindow window = null;
            try {
                window = new GameWindow(music, sfx); window.addNotify(); window.startGame();
                PlayScreen play = (PlayScreen) window.getContentPane().getComponent(1); play.onHide();
                play.setModel(SpecialEnemiesTest.encounter(new NormalEnemy(1, "cat", 100, 150)));
                send(play, "wrong"); send(play, ""); sfx.awaitIdle(); expect(heard);
                send(play, "cat"); sfx.awaitIdle(); expect(heard, Effect.HIT);
                var armored = new ArmoredEnemy(2, "armor", 100, 150);
                play.setModel(SpecialEnemiesTest.encounter(armored));
                send(play, "armor"); sfx.awaitIdle(); expect(heard, Effect.ARMOR);
                send(play, armored.getWord()); sfx.awaitIdle(); expect(heard, Effect.HIT);
                play.setModel(SpecialEnemiesTest.encounter(new BlastEnemy(3, "blast", 100, 150),
                        new BlastEnemy(4, "chain", 130, 150), new NormalEnemy(5, "cat", 140, 150),
                        new NormalEnemy(6, "dog", 800, 150)));
                send(play, "blast"); sfx.awaitIdle(); expect(heard, Effect.BLAST);
                send(play, "dog"); sfx.awaitIdle(); expect(heard, Effect.HIT);
                play.setModel(SpecialEnemiesTest.encounter(new FrostEnemy(7, "ice", 100, 150)));
                button(play, "SLOW").doClick(); sfx.awaitIdle(); expect(heard);
                send(play, "ice"); sfx.awaitIdle(); expect(heard, Effect.HIT);
                button(play, "SLOW").doClick(); sfx.awaitIdle(); expect(heard, Effect.SLOW);
                button(play, "SLOW").doClick(); sfx.awaitIdle(); expect(heard);
                play.setModel(SpecialEnemiesTest.encounter(new NormalEnemy(8, "cat", GameModel.BASE_X, GameModel.BASE_Y)));
                Method tick = PlayScreen.class.getDeclaredMethod("tick"); tick.setAccessible(true);
                tick.invoke(play); sfx.awaitIdle(); expect(heard, Effect.DAMAGE);
                button(play, "PAUSE").doClick(); button(play, "MUTE SFX").doClick();
                check(sfx.isMuted() && !music.isMuted(), "SFX mute is independent");
                button(play, "MUTE MUSIC").doClick(); music.awaitIdle();
                button(play, "UNMUTE SFX").doClick();
                check(!sfx.isMuted() && music.isMuted(), "Music mute is independent");
                for (int width : new int[]{1140, 960}) {
                    window.setSize(width, width == 960 ? 720 : 820); window.validate();
                    for (String prefix : new String[]{"UNMUTE MUSIC", "MUTE SFX", "RESUME", "QUIT GAME"}) {
                        JButton b = button(play, prefix);
                        Rectangle r = SwingUtilities.convertRectangle(b.getParent(), b.getBounds(), play);
                        check(b.getWidth() > 0 && b.getHeight() > 0 && r.y >= 0 && r.getMaxY() <= play.getHeight(), "Pause control fits " + prefix);
                        check(b.getFontMetrics(b.getFont()).stringWidth(b.getText()) <= b.getWidth() - 12, "Text fits " + prefix);
                    }
                    UiSmokeTest.capture(window, "sfx-preview-pause-" + width);
                }
                button(play, "MUTE SFX").doClick(); button(play, "MAIN MENU").doClick();
                window.startGame(); play.onHide();
                check(sfx.isMuted() && music.isMuted(), "Preferences survive menu and replay");
                play.setModel(SpecialEnemiesTest.encounter(new NormalEnemy(9, "cat", 100, 150)));
                send(play, "cat"); sfx.awaitIdle(); expect(heard);
                button(play, "PAUSE").doClick(); button(play, "UNMUTE SFX").doClick();
                send(play, "cat"); sfx.awaitIdle(); expect(heard);
                button(play, "RESUME").doClick();
                play.setModel(SpecialEnemiesTest.encounter(new NormalEnemy(10, "dog", 100, 150)));
                send(play, "dog"); sfx.awaitIdle(); expect(heard, Effect.HIT);
                System.out.println("PASS: UI routes five cues; chain deduplication, invalid actions, separate mutes, replay and minimum-size pause.");
            } catch (Throwable error) { failure[0] = error; }
            finally { if (window != null) window.dispose(); else { sfx.close(); music.close(); } }
        });
        if (failure[0] != null) { failure[0].printStackTrace(); System.exit(1); }
        System.exit(0);
    }
}
