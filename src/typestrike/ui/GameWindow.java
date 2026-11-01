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
    private MenuScreen menu;
    private PlayScreen play;
    private GameOverScreen over;
    private GameScreen current;

    public GameWindow() {
        super("Type Strike | Defend the last spell");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildScreens();
        setContentPane(content);
        setMinimumSize(new Dimension(960, 720));
        setSize(1140, 820);
        setLocationRelativeTo(null);
        showScreen("menu", menu);
        setTitle("Type Strike v1.2.0 | Blast & Healer");
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

    @Override public void dispose() {
        if (current != null) current.onHide();
        super.dispose();
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
