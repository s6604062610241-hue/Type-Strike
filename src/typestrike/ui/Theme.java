package typestrike.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

final class Theme {
    // Original v1.0.0 colors. The theme comparison was a temporary experiment.
    static final Color BACKGROUND = new Color(12, 17, 30);
    static final Color PANEL = new Color(21, 29, 47);
    static final Color TEXT = new Color(234, 241, 252);
    static final Color MUTED = new Color(143, 162, 190);
    static final Color GREEN = new Color(107, 244, 179);
    static final Color PURPLE = new Color(183, 157, 255);
    static final Color RED = new Color(255, 119, 139);
    static final Color GOLD = new Color(255, 208, 117);
    static final Color FIELD = new Color(17, 25, 40);
    static final Color GRID = new Color(23, 34, 51);
    static final Color FIELD_BORDER = new Color(38, 51, 68);
    static final Color BASE_GLOW = new Color(26, 48, 54);
    static final Color BASE_RING = new Color(56, 104, 94);
    static final Color SELECTION = new Color(48, 84, 77);
    static final Color SHADOW = new Color(28, 30, 52);
    static final Color METAL = new Color(115, 145, 193);
    static final Color ICE = Color.decode("#83DFFF");
    private static final Font PIXEL = loadFont();
    static final Font MONO = font(16);

    private static Font loadFont() {
        try (var stream = Theme.class.getResourceAsStream("/fonts/PixelifySans.ttf")) {
            if (stream == null) throw new IllegalStateException("Missing bundled pixel font");
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (java.io.IOException | java.awt.FontFormatException failure) {
            throw new IllegalStateException("Cannot load bundled pixel font", failure);
        }
    }

    static Font font(int size) { return PIXEL.deriveFont(Font.PLAIN, Math.max(12, size)); }

    private Theme() { }

    static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size));
        label.setForeground(color);
        label.setAlignmentX(0.5f);
        return label;
    }

    static JButton button(String text, Color accent) {
        JButton button = new StoneButton(text);
        button.setFont(MONO);
        button.setForeground(accent);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(0.5f);
        return button;
    }

    /** Code-drawn stone bevel; standard JButton actions, focus and keyboard behavior remain. */
    private static final class StoneButton extends JButton {
        StoneButton(String text) { super(text); }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            int w = getWidth(), h = getHeight();
            boolean down = getModel().isPressed() && getModel().isArmed();
            int shift = down ? 2 : 0;
            g.setColor(new Color(9, 10, 15));
            g.fillRect(3, 4, w - 6, h - 4);
            int bottom = h - 5;
            g.setColor(isEnabled() ? new Color(53, 49, 48) : new Color(35, 34, 38));
            g.fillRect(3, shift + 2, w - 6, bottom - 2);
            g.setColor(down ? new Color(30, 29, 33) : new Color(110, 98, 82));
            g.fillRect(6, shift, w - 12, 3);
            g.fillRect(2, shift + 3, 3, bottom - 4);
            g.setColor(new Color(24, 23, 29));
            g.fillRect(w - 5, shift + 3, 3, bottom - 3);
            g.fillRect(5, bottom + shift, w - 10, 3);
            g.setColor(new Color(76, 68, 61));
            g.drawLine(7, 6 + shift, w - 8, 6 + shift);
            if (isEnabled() && (getModel().isRollover() || isFocusOwner())) {
                g.setColor(getForeground());
                g.drawRect(6, 5 + shift, w - 13, h - 14);
            }
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setFont(getFont());
            int x = (w - g.getFontMetrics().stringWidth(getText())) / 2;
            int y = (h - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent() + shift - 1;
            g.setColor(SHADOW); g.drawString(getText(), x + 1, y + 2);
            g.setColor(isEnabled() ? getForeground() : MUTED);
            g.drawString(getText(), x, y);
            g.dispose();
        }
    }
}
