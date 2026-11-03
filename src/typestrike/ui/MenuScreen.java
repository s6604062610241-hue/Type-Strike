package typestrike.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** จัดเมนูตามภาพร่างที่เลือก มีชื่อเกม ปุ่ม ตัวละคร และข้อมูลสองกลุ่มด้านล่าง */
public final class MenuScreen extends GameScreen {
    private final JLabel[] scores = new JLabel[3];
    private final JLabel warning = Theme.label("", 10, Theme.RED);
    private double animationTime;
    private final javax.swing.Timer animation = new javax.swing.Timer(120, e -> {
        animationTime += 0.12;
        repaint();
    });

    public MenuScreen(GameWindow window) {
        super(window);
        setLayout(new BorderLayout(0, 22));
        setBorder(BorderFactory.createEmptyBorder(34, 28, 28, 28));
        JPanel branding = column();
        JLabel logo = Theme.label("TYPE STRIKE", 58, Theme.TEXT);
        logo.setName("menu-logo");
        branding.add(logo);
        branding.add(Box.createVerticalStrut(12));
        branding.add(Theme.label("TYPE / DEFEND / SURVIVE", 14, Theme.GREEN));
        add(branding, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(0, 0, 0, 18);
        c.gridx = 0; c.weightx = 0.3;
        body.add(leaderboard(), c);

        JPanel actions = column();
        actions.setName("menu-actions");
        actions.add(Box.createVerticalGlue());
        JButton start = Theme.button("START SURVIVAL  >", Theme.GREEN);
        start.setMaximumSize(new Dimension(310, 52));
        start.setPreferredSize(new Dimension(270, 52));
        start.addActionListener(e -> window.startGame());
        actions.add(start);
        actions.add(Box.createVerticalStrut(12));
        JButton quit = Theme.button("QUIT", Theme.MUTED);
        quit.setMaximumSize(new Dimension(310, 46));
        quit.setPreferredSize(new Dimension(270, 46));
        quit.addActionListener(e -> window.dispose());
        actions.add(quit);
        actions.add(Box.createVerticalStrut(38));
        JPanel sprites = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                PixelArt.wizard(g, getWidth() / 2 - 32, 10, 4, animationTime);
                int gap = Math.min(58, (getWidth() - 48) / 4);
                int first = (getWidth() - gap * 4 - 48) / 2;
                for (int index = 0; index < 5; index++) {
                    int x = first + index * gap;
                    PixelArt.enemy(g, x, 108, 3, index == 1, index == 1,
                            index == 2, index == 3, index == 4, animationTime + index * 0.1);
                }
                g.dispose();
            }
        };
        sprites.setName("menu-sprites");
        sprites.setOpaque(false);
        sprites.setPreferredSize(new Dimension(320, 190));
        sprites.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        actions.add(sprites);
        actions.add(Box.createVerticalGlue());
        c.gridx = 1; c.weightx = 0.4;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        body.add(actions, c);

        c.gridx = 2; c.weightx = 0.3;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.SOUTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        body.add(instructions(), c);
        add(body, BorderLayout.CENTER);
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static JPanel card(String name, int width) {
        JPanel card = column();
        card.setName(name);
        card.setOpaque(true);
        card.setBackground(Theme.PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.FIELD_BORDER),
                BorderFactory.createEmptyBorder(18, 16, 18, 16)));
        card.setPreferredSize(new Dimension(width, name.equals("leaderboard") ? 242 : 328));
        return card;
    }

    private JPanel leaderboard() {
        JPanel card = card("leaderboard", 260);
        card.add(Theme.label("LOCAL TOP 3", 16, Theme.GOLD));
        card.add(Box.createVerticalStrut(18));
        for (int i = 0; i < scores.length; i++) {
            scores[i] = Theme.label("---", 18, Theme.TEXT);
            scores[i].setIcon(new CrownIcon(i + 1));
            scores[i].setToolTipText("Place " + (i + 1));
            scores[i].setIconTextGap(16);
            scores[i].setAlignmentX(0f);
            card.add(scores[i]);
            card.add(Box.createVerticalStrut(12));
        }
        card.add(Box.createVerticalGlue());
        warning.setAlignmentX(0f);
        card.add(warning);
        return card;
    }

    private JPanel instructions() {
        JPanel card = card("how-to-play", 260);
        card.add(Theme.label("HOW TO PLAY", 16, Theme.GREEN));
        card.add(Box.createVerticalStrut(12));
        card.add(copy("Type a word + ENTER.\nDefend the center tower.", Theme.TEXT, 2));
        card.add(Box.createVerticalStrut(12));
        card.add(copy("NORMAL   One word.\nARMORED  Break + finish.\nFROST    Gain 1 Slow charge.\nBLAST    Hits nearby foes.\nHEALER   Restores armor.", Theme.MUTED, 6));
        card.add(Box.createVerticalStrut(10));
        card.add(copy("SPACE: 50% speed, 5s.\nStore 3 charges. ESC: pause.", Theme.ICE, 3));
        return card;
    }

    private static JTextArea copy(String text, Color color, int rows) {
        JTextArea area = new JTextArea(text, rows, 1);
        area.setFont(Theme.font(14));
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setAlignmentX(0.5f);
        area.setBorder(null);
        return area;
    }

    private record CrownIcon(int rank) implements Icon {
        public int getIconWidth() { return 30; }
        public int getIconHeight() { return 40; }
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            PixelArt.crown(g, rank, x, y);
            g.dispose();
        }
    }

    @Override public void onShow() {
        animation.start();
        var best = window.getRecords().getScores();
        for (int i = 0; i < scores.length; i++) scores[i].setText(i < best.size() ? String.format("%05d", best.get(i)) : "---");
        warning.setText(window.getRecords().getWarning().isBlank() ? "" : "Score save unavailable");
        warning.setToolTipText(window.getRecords().getWarning());
    }

    @Override public void onHide() { animation.stop(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        PixelArt.background((Graphics2D) graphics, getWidth(), getHeight(), true);
    }
}
