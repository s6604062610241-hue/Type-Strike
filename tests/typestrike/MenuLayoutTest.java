package typestrike;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import typestrike.audio.MusicPlayer;
import typestrike.ui.GameWindow;
import typestrike.ui.PlayScreen;

public final class MenuLayoutTest {
    private static Component named(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container nested) {
                Component found = named(nested, name); if (found != null) return found;
            }
        }
        return null;
    }
    private static JButton button(Container root, String prefix) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton b && b.getText().startsWith(prefix)) return b;
            if (child instanceof Container nested) {
                JButton found = button(nested, prefix); if (found != null) return found;
            }
        }
        return null;
    }
    private static Rectangle bounds(Component component, Container root) {
        return SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), root);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void checkButtons(Container root) {
        for (Component child : root.getComponents()) {
            if (!child.isVisible()) continue;
            if (child instanceof JButton button) {
                check(button.getFont().getFamily().contains("Pixelify"), "Bundled font must load on buttons");
                check(button.getFontMetrics(button.getFont()).stringWidth(button.getText()) <= button.getWidth() - 12,
                        "Button text must fit: " + button.getText());
            }
            if (child instanceof Container nested) checkButtons(nested);
        }
    }
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build/ui-review"));
        Throwable[] error = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            GameWindow window = null;
            MusicPlayer music = new MusicPlayer(track -> new MusicPlayer.Playback() {
                public void startLoop() { }
                public void stop() { }
                public void close() { }
            });
            try {
                window = new GameWindow(music); window.addNotify();
                Container root = window.getContentPane();
                for (int width : new int[]{1140, 960}) {
                    window.setSize(width, width == 960 ? 720 : 820); window.validate();
                    Component logo = named(root, "menu-logo"), sprites = named(root, "menu-sprites");
                    Component left = named(root, "leaderboard"), right = named(root, "how-to-play");
                    Rectangle l = bounds(left, root), r = bounds(right, root);
                    JButton start = button((Container) root.getComponent(0), "START");
                    Rectangle action = bounds(start, root), title = bounds(logo, root);
                    check(Math.abs(action.getCenterX() - title.getCenterX()) < 3, "Logo and buttons share center axis");
                    check(bounds(sprites, root).y > action.y + action.height, "Sprites sit below actions");
                    check(l.x < action.x && r.x > action.x + action.width, "Information cards flank the center");
                    check(l.y + l.height <= root.getHeight() && r.y + r.height <= root.getHeight(), "Cards stay inside minimum window");
                    check(l.y + l.height == r.y + r.height, "Cards align along bottom");
                    int medals = 0;
                    for (Component child : ((Container) left).getComponents()) {
                        if (child instanceof JLabel label && label.getIcon() != null) medals++;
                    }
                    check(medals == 3, "Three rank medal icons");
                    checkButtons((Container) root.getComponent(0));
                    for (Component child : ((Container) right).getComponents()) {
                        if (child instanceof JTextArea area) {
                            var last = area.modelToView2D(area.getDocument().getLength());
                            check(last != null && last.getMaxY() <= area.getHeight() + 1,
                                    "Instructions must not be clipped at minimum size");
                        }
                    }
                    UiSmokeTest.capture(window, width == 960 ? "v1.3.0-menu-small" : "v1.3.0-menu");
                }
                window.startGame();
                PlayScreen play = (PlayScreen) root.getComponent(1); play.onHide();
                JProgressBar hp = (JProgressBar) named(play, "tower-health");
                Rectangle health = bounds(hp, play);
                check(health.x < 40 && health.y < 80 && hp.getValue() == 100, "HP at top left");
                play.setTypedWord("te");
                button(play, "PAUSE").doClick();
                button(play, "MUTE MUSIC").doClick(); music.awaitIdle();
                check(music.isMuted() && play.getModel().isPaused(), "Pause music control changes preference without unpausing");
                UiSmokeTest.capture(window, "v1.3.0-pause-muted");
                checkButtons(play);
                button(play, "RESUME").doClick();
                check(music.isMuted() && play.getTypedWord().equals("te"), "Resume retains mute and typed word");
                UiSmokeTest.capture(window, "v1.3.0-gameplay");
                button(play, "PAUSE").doClick(); button(play, "MAIN MENU").doClick();
                check(music.isMuted(), "Return to menu retains mute");
                window.startGame(); play.onHide(); button(play, "PAUSE").doClick();
                button(play, "UNMUTE MUSIC").doClick();
                check(!music.isMuted(), "New round can unmute");
                UiSmokeTest.capture(window, "v1.3.0-pause");
                System.out.println("PASS: sketch layout at two sizes, rank icons, HP placement and pause/music round trips; five renders.");
            } catch (Throwable failure) { error[0] = failure; }
            finally { if (window != null) window.dispose(); else music.close(); }
        });
        if (error[0] != null) { error[0].printStackTrace(); System.exit(1); }
        System.exit(0);
    }
}
