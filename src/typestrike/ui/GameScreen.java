package typestrike.ui;

import javax.swing.JPanel;

/** Abstract screen lifecycle, independently implemented by every screen. */
public abstract class GameScreen extends JPanel {
    protected final GameWindow window;

    @SuppressWarnings("this-escape") // Swing setters are safe before these concrete screens are shown.
    protected GameScreen(GameWindow window) {
        this.window = window;
        setBackground(Theme.BACKGROUND);
    }

    public abstract void onShow();
    public void onHide() { }
}
