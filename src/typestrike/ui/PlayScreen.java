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
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import typestrike.model.Enemy;
import typestrike.model.GameModel;
import typestrike.model.SlowPowerUp;
import typestrike.model.BlastEnemy;
import typestrike.model.HealerEnemy;

public final class PlayScreen extends GameScreen {
    private final JTextField input = new JTextField();
    private final JLabel hud = Theme.label("", 16, Theme.TEXT);
    private final JProgressBar health = new JProgressBar(0, 100);
    private final JButton musicButton = Theme.button("MUTE MUSIC", Theme.ICE);
    private final JLabel feedback = Theme.label("Type a word and press ENTER", 13, Theme.MUTED);
    private final JButton pause = Theme.button("PAUSE [ESC]", Theme.PURPLE);
    private final JButton slowButton = Theme.button("SLOW [SPACE] 0/3", Theme.ICE);
    private final JPanel pauseMenu = new JPanel();
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
        JPanel identity = new JPanel(new BorderLayout(0, 6));
        identity.setOpaque(false);
        identity.add(Theme.label("TYPE STRIKE", 18, Theme.GREEN), BorderLayout.NORTH);
        health.setName("tower-health");
        health.setStringPainted(true);
        health.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override protected Color getSelectionForeground() { return Theme.BACKGROUND; }
            @Override protected Color getSelectionBackground() { return Theme.TEXT; }
        });
        health.getAccessibleContext().setAccessibleName("Tower health");
        health.setFont(Theme.font(11));
        health.setBackground(Theme.FIELD_BORDER);
        health.setForeground(Theme.GREEN);
        health.setBorderPainted(false);
        health.setPreferredSize(new Dimension(200, 18));
        identity.add(health, BorderLayout.SOUTH);
        header.add(identity, BorderLayout.WEST);
        hud.setFont(Theme.font(16));
        header.add(hud, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        configurePauseMenu();
        arena.setLayout(new GridBagLayout());
        arena.add(pauseMenu);
        add(arena, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(14, 9));
        footer.setBackground(Theme.PANEL);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 22, 14, 22));
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setOpaque(false);
        status.add(feedback, BorderLayout.CENTER);
        slowButton.setFont(Theme.font(13));
        slowButton.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        slowButton.setPreferredSize(new Dimension(230, 30));
        slowButton.setToolTipText("Defeat a crystal-marked Frost Slime, then press Space. Stores up to three charges.");
        slowButton.addActionListener(e -> activateSlow());
        status.add(slowButton, BorderLayout.EAST);
        footer.add(status, BorderLayout.NORTH);
        input.setFont(Theme.font(24));
        input.setBackground(Theme.BACKGROUND);
        input.setForeground(Theme.TEXT);
        input.setCaretColor(Theme.GREEN);
        input.setSelectionColor(Theme.SELECTION);
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
        add(footer, BorderLayout.SOUTH);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "pause");
        getActionMap().put("pause", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { togglePause(); }
        });
        AbstractAction slowAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { activateSlow(); }
        };
        // A typed-space binding consumes the character instead of inserting a space.
        input.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(' '), "activate-slow");
        input.getActionMap().put("activate-slow", slowAction);
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(' '), "activate-slow");
        getActionMap().put("activate-slow", slowAction);
    }

    private void configurePauseMenu() {
        pauseMenu.setLayout(new BoxLayout(pauseMenu, BoxLayout.Y_AXIS));
        pauseMenu.setBackground(Theme.PANEL);
        pauseMenu.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.PURPLE),
                BorderFactory.createEmptyBorder(16, 22, 16, 22)));
        pauseMenu.setPreferredSize(new Dimension(390, 406));
        pauseMenu.add(Box.createVerticalGlue());
        pauseMenu.add(Theme.label("PAUSED", 32, Theme.PURPLE));
        pauseMenu.add(Box.createVerticalStrut(8));
        pauseMenu.add(Theme.label("Game and Slow timer are stopped", 12, Theme.MUTED));
        pauseMenu.add(Box.createVerticalStrut(18));
        JButton resume = Theme.button("RESUME [ESC]", Theme.GREEN);
        resume.setMaximumSize(new Dimension(285, 48));
        resume.addActionListener(e -> togglePause());
        pauseMenu.add(resume);
        pauseMenu.add(Box.createVerticalStrut(10));
        JButton menu = Theme.button("MAIN MENU", Theme.PURPLE);
        menu.setMaximumSize(new Dimension(285, 48));
        menu.addActionListener(e -> window.showMenu());
        pauseMenu.add(menu);
        pauseMenu.add(Box.createVerticalStrut(10));
        musicButton.setMaximumSize(new Dimension(285, 48));
        musicButton.addActionListener(e -> {
            window.getMusic().toggleMuted();
            updateMusicUi();
        });
        pauseMenu.add(musicButton);
        pauseMenu.add(Box.createVerticalStrut(10));
        JButton quit = Theme.button("QUIT GAME", Theme.RED);
        quit.setMaximumSize(new Dimension(285, 48));
        quit.addActionListener(e -> window.dispose());
        pauseMenu.add(quit);
        pauseMenu.add(Box.createVerticalStrut(14));
        pauseMenu.add(Theme.label("MAIN MENU ends the current run", 11, Theme.MUTED));
        pauseMenu.add(Box.createVerticalGlue());
        pauseMenu.setVisible(false);
    }

    public void setModel(GameModel value) {
        model = value;
        effects.clear();
        input.setText("");
        damageFlash = 0;
        feedbackRemaining = 0;
        feedback.setForeground(Theme.MUTED);
        feedback.setText("Type a word and press ENTER.");
        updatePauseUi();
        updateHud();
    }

    public GameModel getModel() { return model; }
    public String getTypedWord() { return input.getText(); }
    public void setTypedWord(String word) { input.setText(word); }

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
                feedback.setText(" ");
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
            Color color = hit.frostReward() ? Theme.ICE : hit.eliminated() ? Theme.GREEN : Theme.GOLD;
            String message = hit.chargeAdded() ? "+" + hit.reward() + " / SLOW READY" : hit.eliminated() ? "+" + hit.reward() : "ARMOR BROKEN";
            effects.add(new Effect(hit.x(), hit.y(), message, color));
            showFeedback(hit.frostReward() ? (hit.chargeAdded() ? "+" + hit.reward() + " / SLOW READY - press SPACE when needed!" :
                    "+" + hit.reward() + " / Slow charges already full (3/3).") : hit.eliminated() ? "SPELL HIT  " + message :
                    "ARMOR BROKEN - type the NEW word to finish it!", color);
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

    private void activateSlow() {
        if (model == null || model.isPaused() || model.isGameOver()) return;
        if (model.activateSlow()) {
            showFeedback("SLOW ACTIVE - enemy movement halved for 5 seconds!", Theme.ICE);
        } else {
            showFeedback(model.isSlowActive() ? "Slow is already active; effects do not stack." :
                    "No charge. Defeat a crystal-marked Frost Slime.", Theme.MUTED);
        }
        updateHud();
        arena.repaint();
        input.requestFocusInWindow();
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
        window.getMusic().setPaused(model.isPaused());
        updateMusicUi();
        input.setEnabled(!model.isPaused());
        pause.setText(model.isPaused() ? "RESUME [ESC]" : "PAUSE [ESC]");
        pauseMenu.setVisible(model.isPaused());
        arena.revalidate();
        updateSlowUi();
        if (!model.isPaused()) input.requestFocusInWindow();
        arena.repaint();
    }

    private void updateMusicUi() {
        musicButton.setText(window.getMusic().isMuted() ? "UNMUTE MUSIC" : "MUTE MUSIC");
        musicButton.setToolTipText(window.getMusic().getWarning().isEmpty() ?
                "Music preference stays when returning to the menu or starting another run." : window.getMusic().getWarning());
    }

    private void updateHud() {
        health.setValue(model.getBase().getHealth());
        health.setString("HP " + model.getBase().getHealth() + " / 100");
        health.setForeground(model.getBase().getHealth() > 30 ? Theme.GREEN : Theme.RED);
        hud.setText(String.format("SCORE %05d   BEST %05d   %02d:%02d   LV %02d",
                model.getScore(), Math.max(window.getRecords().getBest(), model.getScore()),
                (int) model.getElapsed() / 60, (int) model.getElapsed() % 60, model.getLevel()));
        updateSlowUi();
    }

    private void updateSlowUi() {
        slowButton.setEnabled(!model.isPaused() && !model.isGameOver() && model.hasSlowCharge() && !model.isSlowActive());
        slowButton.setText(model.isSlowActive() ? String.format(Locale.ROOT, "SLOW %.1fs | NEXT %d/%d",
                model.getSlowRemaining(), model.getSlowCharges(), SlowPowerUp.MAX_CHARGES) :
                String.format(Locale.ROOT, "SLOW [SPACE] %d/%d%s", model.getSlowCharges(), SlowPowerUp.MAX_CHARGES,
                        model.hasSlowCharge() ? " READY" : ""));
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
            drawCombat(g);
            drawEnemies(g);
            for (Effect effect : effects) {
                g.setColor(new Color(effect.color.getRed(), effect.color.getGreen(), effect.color.getBlue(), (int) (effect.life / 0.48 * 220)));
                g.setStroke(new BasicStroke(3));
                g.drawLine((int) GameModel.BASE_X, (int) GameModel.BASE_Y, (int) effect.x, (int) effect.y);
                g.drawOval((int) effect.x - 24, (int) effect.y - 24, 48, 48);
                g.setFont(Theme.MONO);
                int effectX = Math.max(24, Math.min(GameModel.WIDTH - g.getFontMetrics().stringWidth(effect.text) - 24,
                        (int) effect.x - 30));
                int effectY = Math.max(100, (int) (effect.y - 35 - (0.48 - effect.life) * 50));
                g.drawString(effect.text, effectX, effectY);
            }
            if (damageFlash > 0) {
                g.setColor(new Color(Theme.RED.getRed(), Theme.RED.getGreen(), Theme.RED.getBlue(), (int) (damageFlash * 130)));
                g.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);
            }
            if (model.isPaused()) {
                g.setColor(new Color(Theme.BACKGROUND.getRed(), Theme.BACKGROUND.getGreen(), Theme.BACKGROUND.getBlue(), 235));
                g.fillRect(0, 0, GameModel.WIDTH, GameModel.HEIGHT);
            }
            g.dispose();
        }

        private void drawField(Graphics2D g) {
            PixelArt.background(g, GameModel.WIDTH, GameModel.HEIGHT, false);
            g.setColor(Theme.FIELD_BORDER);
            g.drawRect(14, 14, GameModel.WIDTH - 29, GameModel.HEIGHT - 29);
            if (model.isSlowActive()) {
                g.setColor(Theme.ICE);
                g.setStroke(new BasicStroke(3));
                g.drawRect(16, 16, GameModel.WIDTH - 33, GameModel.HEIGHT - 33);
                g.setStroke(new BasicStroke(1));
                centered(g, String.format(Locale.ROOT, "SLOW ACTIVE  %.1fs", model.getSlowRemaining()), 15, 36, Theme.ICE);
            }
        }

        private void drawBase(Graphics2D g) {
            int cx = (int) GameModel.BASE_X;
            int cy = (int) GameModel.BASE_Y;
            g.setColor(Theme.BASE_GLOW);
            g.fillOval(cx - 72, cy - 50, 144, 105);
            g.setColor(model.isSlowActive() ? Theme.ICE : Theme.BASE_RING);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 9}, 0));
            g.drawOval(cx - 72, cy - 64, 144, 128);
            g.setStroke(new BasicStroke(1));
            PixelArt.wizard(g, cx - 32, cy - 38, 4, model.getElapsed());
        }

        private void drawEnemies(Graphics2D g) {
            String typed = input.getText().trim().toLowerCase(Locale.ROOT);
            List<java.awt.Rectangle> labels = new ArrayList<>();
            g.setFont(Theme.font(12));
            FontMetrics tagMetrics = g.getFontMetrics();
            for (Enemy enemy : model.getEnemies()) {
                EnemyTag tag = enemyTag(enemy, tagMetrics);
                if (tag != null) labels.add(new java.awt.Rectangle(tag.x(), tag.y() - 12, tag.width(), 14));
                labels.add(new java.awt.Rectangle((int) enemy.getX() - 30, (int) enemy.getY() - 38, 60, 82));
            }
            List<Enemy> sorted = model.getEnemies().stream().sorted(Comparator.comparingDouble(
                    e -> Math.hypot(e.getX() - GameModel.BASE_X, e.getY() - GameModel.BASE_Y))).toList();
            g.setFont(Theme.font(20));
            FontMetrics metrics = g.getFontMetrics();
            for (Enemy enemy : sorted) {
                int x = (int) enemy.getX();
                int y = (int) enemy.getY();
                boolean elite = enemy.getKind().equals("ARMORED");
                boolean frost = enemy.dropsSlowCharge();
                boolean blast = enemy.getBlastRadius() > 0;
                boolean healer = enemy instanceof HealerEnemy;
                PixelArt.enemy(g, x - 24, y - 18, 3, enemy.hasArmor(), elite, frost, blast, healer,
                        model.getElapsed() + enemy.getId() * 0.13);
                if (frost || blast || healer) {
                    g.setColor(healer ? Theme.GREEN : blast ? Theme.PURPLE : Theme.ICE);
                    g.setFont(Theme.font(10));
                    EnemyTag tag = enemyTag(enemy, tagMetrics);
                    g.drawString(tag.text(), tag.x(), tag.y());
                    g.setFont(Theme.font(20));
                }
                if (Math.hypot(x - GameModel.BASE_X, y - GameModel.BASE_Y) < 150) {
                    g.setColor(Theme.RED);
                    g.drawLine(x - 14, y + 40, x + 14, y + 40);
                }
                int width = metrics.stringWidth(enemy.getWord()) + 18;
                java.awt.Rectangle label = new java.awt.Rectangle(Math.max(16, Math.min(GameModel.WIDTH - width - 16, x - width / 2)),
                        y < 110 ? y + 74 : y - 72, width, 27);
                label = placeLabel(label, labels);
                labels.add(label);
                Color accent = healer ? Theme.GREEN : blast ? Theme.PURPLE : frost ? Theme.ICE : enemy.hasArmor() ? Theme.GOLD : elite ? Theme.PURPLE : Theme.RED;
                g.setColor(Theme.FIELD_BORDER);
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
                    g.fillRect(x - 8, y + 43, 6, 3);
                    g.fillRect(x + 2, y + 43, 6, 3);
                }
            }
        }

        private record EnemyTag(String text, int x, int y, int width) { }

        private java.awt.Rectangle placeLabel(java.awt.Rectangle desired, List<java.awt.Rectangle> occupied) {
            java.awt.Rectangle best = desired;
            int bestDistance = Integer.MAX_VALUE;
            // Find the closest clear position instead of bouncing between two crowded rows.
            for (int row = -12; row <= 12; row++) for (int column = -1; column <= 1; column++) {
                int x = Math.max(16, Math.min(GameModel.WIDTH - desired.width - 16,
                        desired.x + column * (desired.width + 12)));
                int y = Math.max(18, Math.min(GameModel.HEIGHT - 32, desired.y + row * 29));
                int distance = Math.abs(x - desired.x) + 2 * Math.abs(y - desired.y);
                if (distance >= bestDistance) continue;
                java.awt.Rectangle candidate = new java.awt.Rectangle(x, y, desired.width, desired.height);
                boolean overlaps = false;
                for (java.awt.Rectangle other : occupied) if (candidate.intersects(other)) { overlaps = true; break; }
                if (!overlaps) { best = candidate; bestDistance = distance; }
            }
            return best;
        }

        private EnemyTag enemyTag(Enemy enemy, FontMetrics metrics) {
            String text;
            if (enemy instanceof HealerEnemy healer) {
                text = healer.isPreparingHeal() && model.getHealTarget(healer) != null ?
                        String.format(Locale.ROOT, "HEAL %.1fs", healer.getHealRemaining()) : "HEAL";
            } else if (enemy.getBlastRadius() > 0) text = "BLAST";
            else if (enemy.dropsSlowCharge()) text = "SLOW";
            else return null;
            int width = metrics.stringWidth(text);
            boolean bottom = enemy.getY() > GameModel.HEIGHT - 75;
            int x = (int) enemy.getX() + (bottom ? 36 : -width / 2);
            if (bottom && x + width > GameModel.WIDTH - 16) x = (int) enemy.getX() - 36 - width;
            x = Math.max(16, Math.min(GameModel.WIDTH - width - 16, x));
            return new EnemyTag(text, x, (int) enemy.getY() + (bottom ? 10 : 60), width);
        }

        private void drawCombat(Graphics2D g) {
            for (Enemy enemy : model.getEnemies()) {
                if (enemy.getBlastRadius() > 0) {
                    int radius = (int) enemy.getBlastRadius();
                    g.setColor(new Color(Theme.PURPLE.getRed(), Theme.PURPLE.getGreen(), Theme.PURPLE.getBlue(), 70));
                    g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 8}, 0));
                    g.drawOval((int) enemy.getX() - radius, (int) enemy.getY() - radius, radius * 2, radius * 2);
                }
                if (enemy instanceof HealerEnemy healer && healer.isPreparingHeal()) {
                    Enemy target = model.getHealTarget(healer);
                    if (target != null) {
                        g.setColor(Theme.GREEN);
                        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0));
                        g.drawLine((int) healer.getX(), (int) healer.getY(), (int) target.getX(), (int) target.getY());
                    }
                }
            }
            g.setStroke(new BasicStroke(2));
            for (GameModel.CombatEffect effect : model.getCombatEffects()) {
                if (effect.kind().equals("BLAST")) {
                    double progress = 1 - effect.remaining() / 0.7;
                    int radius = (int) (BlastEnemy.RADIUS * (0.4 + 0.6 * progress));
                    g.setColor(Theme.PURPLE);
                    g.drawOval((int) effect.x() - radius, (int) effect.y() - radius, radius * 2, radius * 2);
                } else {
                    g.setColor(Theme.GREEN);
                    g.drawLine((int) effect.x(), (int) effect.y(), (int) effect.targetX(), (int) effect.targetY());
                    g.setFont(Theme.font(12));
                    g.drawString("ARMOR RESTORED", (int) Math.min(GameModel.WIDTH - 135, effect.targetX() - 40),
                            (int) Math.max(75, effect.targetY() - 60));
                }
            }
            g.setStroke(new BasicStroke(1));
        }

        private void centered(Graphics2D g, String text, int size, int y, Color color) {
            g.setFont(Theme.font(size));
            g.setColor(color);
            g.drawString(text, (GameModel.WIDTH - g.getFontMetrics().stringWidth(text)) / 2, y);
        }
    }
}
