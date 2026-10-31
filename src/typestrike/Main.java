package typestrike;

import javax.swing.SwingUtilities;
import typestrike.ui.GameWindow;

/** Application entry point. All Swing components are created on the event thread. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().setVisible(true));
    }
}
