package typestrike.ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import typestrike.model.GameModel;
import typestrike.model.WordRepository;
import typestrike.storage.HighScoreStore;

public final class GameWindow extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final WordRepository words = new WordRepository();
    private final HighScoreStore records = new HighScoreStore(
            Path.of(System.getProperty("user.home"), ".type-strike", "scores.txt"));
    private MenuScreen menu;
    private PlayScreen play;
    private GameOverScreen over;
    private GameScreen current;
    private GameModel lastResult;
    private boolean lastWasRecord;

    public GameWindow() {
        super("Type Strike | Defend the last spell");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildScreens();
        setContentPane(content);
        setMinimumSize(new Dimension(960, 720));
        setSize(1140, 820);
        setLocationRelativeTo(null);
        showScreen("menu", menu);
        for (int index = 0; index < Theme.Palette.values().length; index++) {
            final int themeIndex = index;
            String action = "preview-theme-" + index;
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke("F" + (index + 1)), action);
            getRootPane().getActionMap().put(action, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { selectTheme(themeIndex); }
            });
        }
        updateTitle();
        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override public void windowLostFocus(java.awt.event.WindowEvent e) {
                if (current == play) play.pauseIfRunning();
            }
        });
    }

    private void buildScreens() {
        menu = new MenuScreen(this);
        play = new PlayScreen(this);
        over = new GameOverScreen(this);
        content.add(menu, "menu");
        content.add(play, "play");
        content.add(over, "over");
    }

    /** Rebuild visual components, while retaining the same game model and typed word. */
    public void selectTheme(int index) {
        if (index < 0 || index >= Theme.Palette.values().length) return;
        Theme.Palette palette = Theme.Palette.values()[index];
        if (Theme.current() == palette) return;
        boolean wasPlaying = current == play;
        boolean wasOver = current == over;
        GameModel round = play.getModel();
        String typed = play.getTypedWord();
        if (current != null) current.onHide();
        Theme.select(palette);
        content.removeAll();
        buildScreens();
        if (round != null) {
            play.setModel(round);
            play.setTypedWord(typed);
        }
        if (lastResult != null) over.setResult(lastResult, lastWasRecord);
        current = null;
        showScreen(wasPlaying ? "play" : wasOver ? "over" : "menu", wasPlaying ? play : wasOver ? over : menu);
        updateTitle();
        content.revalidate();
        content.repaint();
    }

    private void updateTitle() {
        setTitle("Type Strike v1.1.0 | Slow Power-Up | " + Theme.current() + " | F1 / F2 / F3");
    }

    private void showScreen(String name, GameScreen next) {
        if (current != null) current.onHide();
        current = next;
        cards.show(content, name);
        current.onShow(); // Polymorphic screen lifecycle.
    }

    public void startGame() {
        play.setModel(new GameModel(words, new Random()));
        showScreen("play", play);
    }
    public void showMenu() { showScreen("menu", menu); }
    public void finishGame(GameModel model) {
        int previousBest = records.getBest();
        records.record(model.getScore());
        lastResult = model;
        lastWasRecord = model.getScore() > previousBest;
        over.setResult(model, lastWasRecord);
        showScreen("over", over);
    }
    public HighScoreStore getRecords() { return records; }
}
