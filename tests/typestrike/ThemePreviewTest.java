package typestrike;

import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.SwingUtilities;
import typestrike.model.GameModel;
import typestrike.model.WordRepository;
import typestrike.ui.GameWindow;
import typestrike.ui.PlayScreen;

/** Verify theme shortcuts preserve an active round and render each palette for review. */
public final class ThemePreviewTest {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build", "ui-review"));
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            GameWindow window = null;
            try {
                window = new GameWindow();
                window.addNotify();
                String[] names = {"moonlight", "forest", "arcade"};
                for (int i = 0; i < names.length; i++) {
                    window.selectTheme(i);
                    UiSmokeTest.capture(window, names[i] + "-menu");
                    window.setSize(960, 720);
                    UiSmokeTest.capture(window, names[i] + "-menu-minimum");
                    window.setSize(1140, 820);
                }
                window.selectTheme(0);
                window.startGame();
                PlayScreen play = (PlayScreen) window.getContentPane().getComponent(1);
                play.onHide();
                GameModel model = new GameModel(new WordRepository(), new Random(73));
                for (int frame = 0; frame < 130; frame++) model.update(0.1);
                play.setModel(model);
                play.setTypedWord("te");
                double time = model.getElapsed();
                int hp = model.getBase().getHealth();
                int enemies = model.getEnemies().size();
                int score = model.getScore();
                for (int index : new int[]{0, 1, 2, 0}) {
                    window.getRootPane().getActionMap().get("preview-theme-" + index)
                            .actionPerformed(new ActionEvent(window, ActionEvent.ACTION_PERFORMED, "test"));
                    play = (PlayScreen) window.getContentPane().getComponent(1);
                    play.onHide();
                    if (play.getModel() != model || model.getElapsed() != time
                            || model.getBase().getHealth() != hp || model.getScore() != score
                            || model.getEnemies().size() != enemies || !play.getTypedWord().equals("te")) {
                        throw new AssertionError("Theme change modified round or input");
                    }
                    UiSmokeTest.capture(window, names[index] + "-gameplay");
                }
                model.togglePause();
                window.selectTheme(1);
                play = (PlayScreen) window.getContentPane().getComponent(1);
                play.onHide();
                if (!model.isPaused() || play.getModel() != model || !play.getTypedWord().equals("te")) {
                    throw new AssertionError("Theme change did not retain pause state");
                }
                System.out.println("PASS: all three themes, menu and minimum-size layouts; F1/F2/F3 retain round, input and pause.");
            } catch (Throwable problem) { failure[0] = problem; }
            finally { if (window != null) window.dispose(); }
        });
        if (failure[0] != null) {
            failure[0].printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
