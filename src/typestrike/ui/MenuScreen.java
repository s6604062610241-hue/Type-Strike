package typestrike.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

public final class MenuScreen extends GameScreen {
    private final JLabel records = Theme.label("", 16, Theme.GOLD);
    private final JLabel warning = Theme.label("", 11, Theme.RED);

    public MenuScreen(GameWindow window) {
        super(window);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(24, 25, 24, 25));
        add(Box.createVerticalGlue());
        add(Theme.label("T Y P E   /   D E F E N D   /   S U R V I V E", 13, Theme.GREEN));
        add(Box.createVerticalStrut(16));
        add(Theme.label("TYPE STRIKE", 68, Theme.TEXT));
        add(Box.createVerticalStrut(8));
        add(Theme.label("Defend the last spell.", 19, Theme.PURPLE));
        add(Box.createVerticalStrut(105));
        add(Theme.label("Your tower stands at the center. Every word is a spell.", 15, Theme.MUTED));
        add(Box.createVerticalStrut(9));
        add(Theme.label("Type an enemy's word, then press ENTER to strike.", 15, Theme.TEXT));
        add(Box.createVerticalStrut(22));
        JButton start = Theme.button("START SURVIVAL  >", Theme.GREEN);
        start.addActionListener(e -> window.startGame());
        start.setMaximumSize(new Dimension(310, 52));
        add(start);
        add(Box.createVerticalStrut(12));
        JButton quit = Theme.button("QUIT", Theme.MUTED);
        quit.addActionListener(e -> window.dispose());
        quit.setMaximumSize(new Dimension(310, 46));
        add(quit);
        add(Box.createVerticalStrut(25));
        add(records);
        add(Box.createVerticalStrut(20));
        add(Theme.label("PINK: 1 word / +10     GOLD + HELMET: 2 words / +25", 13, Theme.MUTED));
        add(Box.createVerticalStrut(8));
        add(Theme.label("Short words move faster. ESC pauses. Losing focus pauses automatically.", 12, Theme.MUTED));
        add(Box.createVerticalStrut(12));
        add(warning);
        add(Box.createVerticalGlue());
        add(Theme.label("JAVA / OOP MINI PROJECT     -     ORIGINAL PLACEHOLDER PIXEL ART", 11, Theme.MUTED));
    }

    @Override public void onShow() {
        var scores = window.getRecords().getScores();
        records.setText("LOCAL RECORDS   " + (scores.isEmpty() ? "--- / --- / ---" :
                String.join("  /  ", scores.stream().map(String::valueOf).toList())));
        warning.setText(window.getRecords().getWarning());
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(new Color(21, 30, 46));
        for (int x = 0; x < getWidth(); x += 32) g.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 32) g.drawLine(0, y, getWidth(), y);
        int y = Math.max(0, (getHeight() - 760) / 2) + 245;
        PixelArt.wizard(g, getWidth() / 2 - 32, y, 4);
        PixelArt.enemy(g, getWidth() / 2 - 175, y + 8, 3, false, false);
        PixelArt.enemy(g, getWidth() / 2 + 130, y + 8, 3, true, true);
        g.dispose();
    }
}
