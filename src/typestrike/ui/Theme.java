package typestrike.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
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
    static final Font MONO = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private Theme() { }

    static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, size));
        label.setForeground(color);
        label.setAlignmentX(0.5f);
        return label;
    }

    static JButton button(String text, Color accent) {
        JButton button = new JButton(text);
        button.setFont(MONO);
        button.setBackground(PANEL);
        button.setForeground(accent);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(accent, 1),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(0.5f);
        return button;
    }
}
