package typestrike;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
    private static <T extends Component> T find(Container root, Class<T> type, String prefix) {
        for (Component child : root.getComponents()) {
            if (!child.isVisible()) continue;
            if (type.isInstance(child) && (!(child instanceof JButton b) || b.getText().startsWith(prefix))) return type.cast(child);
            if (child instanceof Container nested) {
                T found = find(nested, type, prefix);
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
                window = new GameWindow(); window.addNotify();
                if (find(window.getContentPane(), JComboBox.class, "") != null
                        || window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("F1")) != null) {
                    throw new AssertionError("Theme preview controls must not ship");
                }
                UiSmokeTest.capture(window, "v1.1.1-menu");
                window.startGame();
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
                UiSmokeTest.capture(window, "v1.1.1-frost-enemy");
                JTextField field = find(play, JTextField.class, "");
                field.setText(frost.getWord()); field.postActionEvent();
                if (model.getSlowCharges() != 1 || model.getScore() != 20) throw new AssertionError("Enter must grant charge and points");
                field.setText("te");
                Object binding = field.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(' '));
                field.getActionMap().get(binding).actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, "typed space"));
                if (!model.isSlowActive() || model.hasSlowCharge() || !field.getText().equals("te")
                        || model.getSlowRemaining() != 5 || model.getScore() != 20) {
                    throw new AssertionError("Space must activate five seconds without inserting or submitting text");
                }
                UiSmokeTest.capture(window, "v1.1.1-slow-active");
                find(play, JButton.class, "PAUSE").doClick();
                double time = model.getElapsed();
                model.update(10);
                if (!model.isPaused() || model.getElapsed() != time || model.getSlowRemaining() != 5) {
                    throw new AssertionError("Pause must freeze game and Slow timers");
                }
                UiSmokeTest.capture(window, "v1.1.1-pause-menu");
                window.setSize(960, 720);
                UiSmokeTest.capture(window, "v1.1.1-pause-minimum");
                find(play, JButton.class, "RESUME").doClick();
                if (model.isPaused() || !field.getText().equals("te")) throw new AssertionError("Resume must retain input and round");
                find(play, JButton.class, "PAUSE").doClick();
                find(play, JButton.class, "MAIN MENU").doClick();
                if (!window.getContentPane().getComponent(0).isVisible()) throw new AssertionError("Main Menu button did not leave round");
                window.startGame(); play.onHide();
                if (play.getModel() == model || play.getModel().getElapsed() != 0 || play.getModel().getSlowCharges() != 0) {
                    throw new AssertionError("Starting after Main Menu must create a fresh round");
                }
                find(play, JButton.class, "PAUSE").doClick();
                find(play, JButton.class, "QUIT GAME").doClick();
                if (window.isDisplayable()) throw new AssertionError("Quit did not dispose the game");
                System.out.println("PASS: five-second Space, pause Resume/Main Menu/Quit, fresh round, no theme controls; five renders.");
            } catch (Throwable problem) { failure[0] = problem; }
            finally { if (window != null) window.dispose(); }
        });
        if (failure[0] != null) { failure[0].printStackTrace(); System.exit(1); }
        System.exit(0);
    }
}
