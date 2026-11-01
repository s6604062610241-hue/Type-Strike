package typestrike;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import typestrike.model.*;
import typestrike.ui.*;

public final class SpecialEnemiesUiTest {
    private static JTextField input(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTextField field) return field;
            if (child instanceof Container nested) {
                JTextField field = input(nested);
                if (field != null) return field;
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build/ui-review"));
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            GameWindow window = null;
            try {
                window = new GameWindow(); window.addNotify();
                UiSmokeTest.capture(window, "v1.2.0-menu");
                window.setSize(960, 720);
                UiSmokeTest.capture(window, "v1.2.0-menu-small");
                window.setSize(1140, 820);
                window.startGame();
                PlayScreen play = (PlayScreen) window.getContentPane().getComponent(1); play.onHide();
                ArmoredEnemy exposed = new ArmoredEnemy(6, "shield", 310, 350);
                exposed.onWordMatched(new WordRepository(), new java.util.Random(5), java.util.Set.of("shield", "cat", "spark", "blast", "healer", "ice"));
                HealerEnemy healer = new HealerEnemy(5, "healer", 210, 350);
                healer.advanceHealing(6.6);
                GameModel model = SpecialEnemiesTest.encounter(new BlastEnemy(1, "blast", 720, 150),
                        new NormalEnemy(2, "cat", 820, 150), new ArmoredEnemy(3, "armor", 750, 240),
                        new FrostEnemy(4, "ice", 200, 130), healer, exposed);
                play.setModel(model);
                UiSmokeTest.capture(window, "v1.2.0-special-enemies");
                JTextField field = input(play);
                field.setText("blast"); field.postActionEvent();
                if (model.getScore() != 25 || !field.getText().isEmpty()) throw new AssertionError("Blast Enter must clear input and score area kills");
                UiSmokeTest.capture(window, "v1.2.0-blast");
                model.update(1.4);
                if (!exposed.hasArmor()) throw new AssertionError("Visible healer encounter did not restore armor");
                UiSmokeTest.capture(window, "v1.2.0-heal");
                window.setSize(960, 720);
                UiSmokeTest.capture(window, "v1.2.0-play-small");
                System.out.println("PASS: new enemy Enter interaction, restoration, six UI renders at normal/minimum sizes.");
            } catch (Throwable error) { failure[0] = error; }
            finally { if (window != null) window.dispose(); }
        });
        if (failure[0] != null) { failure[0].printStackTrace(); System.exit(1); }
        System.exit(0);
    }
}
