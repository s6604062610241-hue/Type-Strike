package typestrike.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import typestrike.model.GameModel;

public final class GameOverScreen extends GameScreen {
    private final JLabel title = Theme.label("", 15, Theme.GOLD);
    private final JLabel score = Theme.label("", 64, Theme.GREEN);
    private final JLabel stats = Theme.label("", 15, Theme.MUTED);
    private final JLabel best = Theme.label("", 17, Theme.GOLD);
    private final JLabel warning = Theme.label("", 12, Theme.RED);

    public GameOverScreen(GameWindow window) {
        super(window);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        add(Box.createVerticalGlue());
        add(title);
        add(Box.createVerticalStrut(18));
        add(Theme.label("THE SPELL IS BROKEN", 40, Theme.TEXT));
        add(Box.createVerticalStrut(12));
        add(Theme.label("GAME OVER", 18, Theme.RED));
        add(Box.createVerticalStrut(34));
        add(score);
        add(Theme.label("POINTS", 14, Theme.MUTED));
        add(Box.createVerticalStrut(22));
        add(stats);
        add(Box.createVerticalStrut(16));
        add(best);
        add(Box.createVerticalStrut(32));
        JButton again = Theme.button("PLAY AGAIN  >", Theme.GREEN);
        again.addActionListener(e -> window.startGame());
        add(again);
        add(Box.createVerticalStrut(14));
        JButton menu = Theme.button("MAIN MENU", Theme.PURPLE);
        menu.addActionListener(e -> window.showMenu());
        add(menu);
        add(Box.createVerticalStrut(14));
        JButton quit = Theme.button("QUIT", Theme.MUTED);
        quit.addActionListener(e -> window.dispose());
        add(quit);
        add(Box.createVerticalStrut(20));
        add(warning);
        add(Box.createVerticalGlue());
    }

    public void setResult(GameModel model, boolean newBest) {
        title.setText(newBest ? "* NEW PERSONAL RECORD *" : "THE LAST TOWER HAS FALLEN");
        score.setText(String.valueOf(model.getScore()));
        stats.setText(String.format("SURVIVED %02d:%02d    DEFEATED %d    MATCH RATE %d%%",
                (int) model.getElapsed() / 60, (int) model.getElapsed() % 60, model.getDefeated(), model.getAccuracy()));
        best.setText("BEST " + window.getRecords().getBest());
        warning.setText(window.getRecords().getWarning());
    }

    @Override public void onShow() { }
}
