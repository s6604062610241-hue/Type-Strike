package typestrike.ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import typestrike.model.GameModel;
import typestrike.model.WordRepository;
import typestrike.storage.HighScoreStore;

public final class GameWindow extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final WordRepository words = new WordRepository();
    private final HighScoreStore records = new HighScoreStore(
            Path.of(System.getProperty("user.home"), ".type-strike", "scores.txt"));
    private final MenuScreen menu = new MenuScreen(this);
    private final PlayScreen play = new PlayScreen(this);
    private final GameOverScreen over = new GameOverScreen(this);
    private GameScreen current;

    public GameWindow() {
        super("Type Strike | Defend the last spell");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        content.add(menu, "menu");
        content.add(play, "play");
        content.add(over, "over");
        setContentPane(content);
        setMinimumSize(new Dimension(960, 720));
        setSize(1140, 820);
        setLocationRelativeTo(null);
        showScreen("menu", menu);
        addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override public void windowLostFocus(java.awt.event.WindowEvent e) {
                if (current == play) play.pauseIfRunning();
            }
        });
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
        over.setResult(model, model.getScore() > previousBest);
        showScreen("over", over);
    }
    public HighScoreStore getRecords() { return records; }
}
