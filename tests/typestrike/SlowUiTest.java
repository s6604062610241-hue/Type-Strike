package typestrike;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import typestrike.model.Enemy;
import typestrike.model.GameModel;
import typestrike.model.WordRepository;
import typestrike.ui.GameWindow;
import typestrike.ui.PlayScreen;

public final class SlowUiTest {
    private static JTextField input(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTextField field) return field;
            if (child instanceof Container nested) {
                JTextField found = input(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build", "ui-review"));
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            GameWindow window = null;
            try {
                window = new GameWindow(); window.addNotify(); window.startGame();
                PlayScreen play = (PlayScreen) window.getContentPane().getComponent(1);
                play.onHide();
                GameModel model = new GameModel(new WordRepository(), new Random(73));
                Enemy frost = null;
                for (int frame = 0; frame < 1000 && frost == null; frame++) {
                    model.update(0.1);
                    frost = model.getEnemies().stream().filter(Enemy::dropsSlowCharge).findFirst().orElse(null);
                }
                if (frost == null) throw new AssertionError("No Frost enemy");
                play.setModel(model);
                UiSmokeTest.capture(window, "v1.1-frost-enemy");
                JTextField field = input(play);
                field.setText(frost.getWord()); field.postActionEvent();
                if (!model.hasSlowCharge() || model.getScore() != 20) throw new AssertionError("Enter did not grant charge and points");
                UiSmokeTest.capture(window, "v1.1-slow-ready");
                field.setText("te");
                Object binding = field.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(' '));
                field.getActionMap().get(binding).actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, "typed space"));
                if (!model.isSlowActive() || model.hasSlowCharge() || !field.getText().equals("te") || model.getScore() != 20) {
                    throw new AssertionError("Space must consume charge without inserting or submitting a word");
                }
                String[] names = {"moonlight", "forest", "arcade"};
                for (int i = 0; i < 3; i++) {
                    window.selectTheme(i);
                    play = (PlayScreen) window.getContentPane().getComponent(1);
                    play.onHide();
                    if (play.getModel() != model || !model.isSlowActive() || model.getSlowRemaining() != 4
                            || !play.getTypedWord().equals("te")) throw new AssertionError("Theme lost active power or input");
                    UiSmokeTest.capture(window, "v1.1-slow-active-" + names[i]);
                }
                window.setSize(960, 720);
                UiSmokeTest.capture(window, "v1.1-slow-minimum");
                model.togglePause();
                UiSmokeTest.capture(window, "v1.1-slow-paused");
                System.out.println("PASS: Frost Enter reward, Space binding, input preservation, active Slow across three themes; seven renders.");
            } catch (Throwable problem) { failure[0] = problem; }
            finally { if (window != null) window.dispose(); }
        });
        if (failure[0] != null) { failure[0].printStackTrace(); System.exit(1); }
        System.exit(0);
    }
}
