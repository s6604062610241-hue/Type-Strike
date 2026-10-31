package typestrike.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import typestrike.model.Enemy;
import typestrike.model.GameModel;

public final class PlayScreen extends GameScreen {
    private final JTextField input = new JTextField();
    private final JLabel hud = Theme.label("", 16, Theme.TEXT);
    private final JLabel feedback = Theme.label("Type a word and press ENTER", 13, Theme.MUTED);
    private final JButton pause = Theme.button("PAUSE [ESC]", Theme.PURPLE);
    private final Arena arena = new Arena();
    private final Timer timer = new Timer(16, e -> tick());
    private final List<Effect> effects = new ArrayList<>();
    private GameModel model;
    private long previousNanos;
    private double feedbackRemaining;
    private double damageFlash;

    private static final class Effect {
        final double x;
        final double y;
        final String text;
        final Color color;
        double life = 0.48;
        Effect(double x, double y, String text, Color color) {
            this.x = x; this.y = y; this.text = text; this.color = color;
        }
    }

    public PlayScreen(GameWindow window) {
        super(window);
        setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Theme.PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(15, 22, 15, 22));
        header.add(Theme.label("TYPE STRIKE", 21, Theme.GREEN), BorderLayout.WEST);
        header.add(hud, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(arena, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(14, 9));
        footer.setBackground(Theme.PANEL);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 22, 14, 22));
        footer.add(feedback, BorderLayout.NORTH);
        input.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        input.setBackground(Theme.BACKGROUND);
        input.setForeground(Theme.TEXT);
        input.setCaretColor(Theme.GREEN);
        input.setSelectionColor(new Color(48, 84, 77));
        input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.GREEN, 2),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)));
        input.getAccessibleContext().setAccessibleName("Enemy word");
        input.setToolTipText("Type the word over an enemy, then press Enter");
        input.addActionListener(e -> submit());
        // Prevent a pasted paragraph from obscuring the entire input field.
        ((javax.swing.text.AbstractDocument) input.getDocument()).setDocumentFilter(new javax.swing.text.DocumentFilter() {
            @Override public void insertString(FilterBypass fb, int offset, String text,
                    javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                replace(fb, offset, 0, text, attr);
            }
            @Override public void replace(FilterBypass fb, int offset, int length, String text,
                    javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
                String value = text == null ? "" : text;
                if (fb.getDocument().getLength() - length + value.length() <= 32) {
                    super.replace(fb, offset, length, value, attr);
                }
            }
        });
        footer.add(input, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cast = Theme.button("CAST [ENTER]", Theme.GREEN);
        cast.addActionListener(e -> submit());
        pause.addActionListener(e -> togglePause());
        buttons.add(cast);
        buttons.add(pause);
        footer.add(buttons, BorderLayout.EAST);
        footer.add(Theme.label("PINK: one hit   |   HELMET: two different words   |   Hits to your tower cost word length in HP", 11, Theme.MUTED), BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "pause");
        getActionMap().put("pause", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { togglePause(); }
        });
    }

    public void setModel(GameModel value) {
        model = value;
        effects.clear();
        input.setText("");
        damageFlash = 0;
        feedbackRemaining = 0;
        feedback.setForeground(Theme.MUTED);
        feedback.setText("Defend the center. Type an enemy's word, then press ENTER.");
        updatePauseUi();
        updateHud();
    }

    @Override public void onShow() {
        previousNanos = System.nanoTime();
        timer.start();
        SwingUtilities.invokeLater(() -> input.requestFocusInWindow());
    }

    @Override public void onHide() { timer.stop(); }

    private void tick() {
        long now = System.nanoTime();
        // Cap a single frame so a debugger/window stall cannot teleport enemies.
        double seconds = Math.min(0.1, (now - previousNanos) / 1_000_000_000.0);
        previousNanos = now;
        if (!model.isPaused()) {
            int hp = model.getBase().getHealth();
            model.update(seconds);
            if (hp > model.getBase().getHealth()) {
                damageFlash = 0.32;
                showFeedback("TOWER HIT  -" + (hp - model.getBase().getHealth()) + " HP", Theme.RED);
            }
            effects.forEach(effect -> effect.life -= seconds);
            effects.removeIf(effect -> effect.life <= 0);
            damageFlash = Math.max(0, damageFlash - seconds);
            feedbackRemaining -= seconds;
            if (feedbackRemaining <= 0 && model.getElapsed() > 3) {
                feedback.setForeground(Theme.MUTED);
                feedback.setText("Choose your priority: distance, word length, and armor.");
            }
        }
        updateHud();
        arena.repaint();
        if (model.isGameOver()) window.finishGame(model);
    }

    private void submit() {
        if (model == null || model.isPaused() || model.isGameOver()) return;
        String text = input.getText();
        if (text.isBlank()) return;
        GameModel.HitResult hit = model.submit(text);
        input.setText("");
        if (hit.matched()) {
            Color color = hit.eliminated() ? Theme.GREEN : Theme.GOLD;
            String message = hit.eliminated() ? "+" + hit.reward() : "ARMOR BROKEN";
            effects.add(new Effect(hit.x(), hit.y(), message, color));
            showFeedback(hit.eliminated() ? "SPELL HIT  " + message : "ARMOR BROKEN - type the NEW word to finish it!", color);
        } else {
            showFeedback("NO MATCH - check the word and try again. No HP penalty.", Theme.RED);
        }
        updateHud();
        arena.repaint();
        input.requestFocusInWindow();
    }

    private void showFeedback(String text, Color color) {
        feedback.setText(text);
        feedback.setForeground(color);
        feedbackRemaining = 2.3;
    }

    private void togglePause() {
        if (model == null || model.isGameOver()) return;
        model.togglePause();
        updatePauseUi();
    }

    public void pauseIfRunning() {
        if (model != null && !model.isPaused() && !model.isGameOver()) togglePause();
    }

    private void updatePauseUi() {
        input.setEnabled(!model.isPaused());
        pause.setText(model.isPaused() ? "RESUME [ESC]" : "PAUSE [ESC]");
        if (!model.isPaused()) input.requestFocusInWindow();
        arena.repaint();
    }

    private void updateHud() {
        hud.setText(String.format("SCORE %05d   BEST %05d   %02d:%02d   LV %02d",
                model.getScore(), Math.max(window.getRecords().getBest(), model.getScore()),
                (int) model.getElapsed() / 60, (int) model.getElapsed() % 60, model.getLevel()));
    }

    /** Scaled, letterboxed world; resizing never changes game coordinates or speed. */
    private final class Arena extends JPanel {
        Arena() {
            setBackground(Theme.BACKGROUND);
            setPreferredSize(new Dimension(GameModel.WIDTH, GameModel.HEIGHT));
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (model == null) return;
            Graphics2D g = (Graphics2D) graphics.create();
            double scale = Math.min(getWidth() / (double) GameModel.WIDTH, getHeight() / (double) GameModel.HEIGHT);
            g.translate((getWidth() - GameModel.WIDTH * scale) / 2, (getHeight() - GameModel.HEIGHT * scale) / 2);
            g.scale(scale, scale);
            g.setClip(0, 0, GameModel.WIDTH, GameModel.HEIGHT);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawField(g);
            drawBase(g);
            drawEnemies(g);
            for (Effect effect : effects) {
                g.setColor(new Color(effect.color.getRed(), effect.color.getGreen(), effect.color.getBlue(), (int) (effect.life / 0.48 * 220)));
                g.setStroke(new BasicStroke(3));
                g.drawLine((int) GameModel.BASE_X, (int) GameModel.BASE_Y, (int) effect.x, (int) effect.y);
                g.drawOval((int) effect.x - 24, (int) effect.y - 24, 48, 48);
                g.setFont(Theme.MONO);
                g.drawString(effect.text, (int) effect.x - 30, (int) (effect.y - 35 - (0.48 - effect.life) * 50));
            }
            if (damageFlash > 0) {
                g.setColor(new Color(255, 80, 100, (int) (damageFlash * 130)));
                g.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);
            }
            if (model.isPaused()) {
                g.setColor(new Color(7, 11, 22, 225));
                g.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);
                centered(g, "PAUSED", 52, 245, Theme.PURPLE);
                centered(g, "Press ESC or RESUME to continue", 18, 292, Theme.TEXT);
                centered(g, "The clock and enemies are stopped.", 13, 323, Theme.MUTED);
            }
            g.dispose();
        }

        private void drawField(Graphics2D g) {
            g.setColor(new Color(17, 25, 40));
            g.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);
            g.setColor(new Color(23, 34, 51));
            for (int x = 0; x < GameModel.WIDTH; x += 28) g.drawLine(x, 0, x, GameModel.HEIGHT);
            for (int y = 0; y < GameModel.HEIGHT; y += 28) g.drawLine(0, y, GameModel.WIDTH, y);
            g.setColor(new Color(38, 51, 68));
            g.drawRect(14, 14, GameModel.WIDTH - 29, GameModel.HEIGHT - 29);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
            g.setColor(Theme.MUTED);
            g.drawString("PERIMETER / HOSTILES " + model.getEnemies().size(), 30, 36);
            String hp = "TOWER " + model.getBase().getHealth() + "/100";
            g.setColor(Theme.TEXT);
            g.drawString(hp, 860, 35);
            g.setColor(new Color(44, 52, 69));
            g.fillRect(860, 43, 200, 9);
            g.setColor(model.getBase().getHealth() > 30 ? Theme.GREEN : Theme.RED);
            g.fillRect(860, 43, model.getBase().getHealth() * 2, 9);
        }

        private void drawBase(Graphics2D g) {
            int cx = (int) GameModel.BASE_X;
            int cy = (int) GameModel.BASE_Y;
            g.setColor(new Color(26, 48, 54));
            g.fillOval(cx - 72, cy - 50, 144, 105);
            g.setColor(new Color(56, 104, 94));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 9}, 0));
            g.drawOval(cx - 72, cy - 64, 144, 128);
            g.setStroke(new BasicStroke(1));
            PixelArt.wizard(g, cx - 32, cy - 38, 4);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
            g.setColor(Theme.GREEN);
            g.drawString("THE LAST TOWER", cx - 46, cy + 83);
        }

        private void drawEnemies(Graphics2D g) {
            String typed = input.getText().trim().toLowerCase(Locale.ROOT);
            List<java.awt.Rectangle> labels = new ArrayList<>();
            List<Enemy> sorted = model.getEnemies().stream().sorted(Comparator.comparingDouble(
                    e -> Math.hypot(e.getX() - GameModel.BASE_X, e.getY() - GameModel.BASE_Y))).toList();
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
            FontMetrics metrics = g.getFontMetrics();
            for (Enemy enemy : sorted) {
                int x = (int) enemy.getX();
                int y = (int) enemy.getY();
                boolean elite = enemy.getKind().equals("ARMORED");
                PixelArt.enemy(g, x - 24, y - 18, 3, enemy.hasArmor(), elite);
                if (Math.hypot(x - GameModel.BASE_X, y - GameModel.BASE_Y) < 150) {
                    g.setColor(Theme.RED);
                    g.drawLine(x - 14, y + 27, x + 14, y + 27);
                }
                int width = metrics.stringWidth(enemy.getWord()) + 18;
                java.awt.Rectangle label = new java.awt.Rectangle(Math.max(16, Math.min(GameModel.WIDTH - width - 16, x - width / 2)),
                        Math.max(54, y - 48), width, 27);
                // Move overlapping labels upward, and draw a connector to their enemy.
                for (int attempt = 0; attempt < 7; attempt++) {
                    boolean overlaps = false;
                    for (java.awt.Rectangle other : labels) if (label.intersects(other)) overlaps = true;
                    if (!overlaps) break;
                    label.y = label.y > 80 ? label.y - 29 : label.y + 58;
                    label.y = Math.min(GameModel.HEIGHT - 32, label.y);
                }
                labels.add(label);
                Color accent = enemy.hasArmor() ? Theme.GOLD : elite ? Theme.PURPLE : Theme.RED;
                g.setColor(new Color(47, 58, 78));
                g.drawLine(label.x + width / 2, label.y + 27, x, y - 12);
                g.setColor(Theme.BACKGROUND);
                g.fillRect(label.x, label.y, width, 27);
                g.setColor(accent);
                g.drawRect(label.x, label.y, width, 27);
                g.setColor(Theme.TEXT);
                g.drawString(enemy.getWord(), label.x + 9, label.y + 19);
                if (!typed.isEmpty() && enemy.getWord().startsWith(typed)) {
                    g.setColor(Theme.GREEN);
                    g.drawString(typed, label.x + 9, label.y + 19);
                }
                if (enemy.hasArmor()) {
                    g.setColor(Theme.GOLD);
                    g.fillRect(x - 8, y + 30, 6, 3);
                    g.fillRect(x + 2, y + 30, 6, 3);
                }
            }
        }

        private void centered(Graphics2D g, String text, int size, int y, Color color) {
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, size));
            g.setColor(color);
            g.drawString(text, (GameModel.WIDTH - g.getFontMetrics().stringWidth(text)) / 2, y);
        }
    }
}
