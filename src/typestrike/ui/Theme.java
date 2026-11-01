package typestrike.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

final class Theme {
    enum Palette {
        MOONLIGHT("1 / 3  Moonlight Magic", "#101827", "#1B2540", "#F4F1E8", "#A5AEC8", "#6EE7B7", "#A78BFA", "#FF788B", "#F4CB79"),
        FOREST("2 / 3  Enchanted Forest", "#142D26", "#203E32", "#F2E8CF", "#AEC5B0", "#91D1A0", "#E9C46A", "#F38B77", "#D9AC65"),
        ARCADE("3 / 3  Retro Arcade", "#15152D", "#252543", "#F3F6FF", "#B4B5D8", "#67DFF0", "#FF76AD", "#FF927A", "#FFE08A");

        private final String display;
        private final String[] colors;

        Palette(String display, String... colors) { this.display = display; this.colors = colors; }
        @Override public String toString() { return display; }
    }

    static Color BACKGROUND;
    static Color PANEL;
    static Color TEXT;
    static Color MUTED;
    static Color GREEN;
    static Color PURPLE;
    static Color RED;
    static Color GOLD;
    static Color FIELD;
    static Color GRID;
    static Color FIELD_BORDER;
    static Color BASE_GLOW;
    static Color BASE_RING;
    static Color SELECTION;
    static Color SHADOW;
    static Color METAL;
    static Color ICE;
    private static Palette current;
    static final Font MONO = new Font(Font.MONOSPACED, Font.BOLD, 16);

    static { select(Palette.MOONLIGHT); }

    private Theme() { }

    static Palette current() { return current; }

    static void select(Palette palette) {
        current = palette;
        BACKGROUND = Color.decode(palette.colors[0]);
        PANEL = Color.decode(palette.colors[1]);
        TEXT = Color.decode(palette.colors[2]);
        MUTED = Color.decode(palette.colors[3]);
        GREEN = Color.decode(palette.colors[4]);
        PURPLE = Color.decode(palette.colors[5]);
        RED = Color.decode(palette.colors[6]);
        GOLD = Color.decode(palette.colors[7]);
        FIELD = blend(BACKGROUND, PANEL, 0.25);
        GRID = blend(FIELD, MUTED, 0.09);
        FIELD_BORDER = blend(FIELD, MUTED, 0.25);
        BASE_GLOW = blend(PANEL, GREEN, 0.13);
        BASE_RING = blend(PANEL, GREEN, 0.42);
        SELECTION = blend(PANEL, GREEN, 0.32);
        SHADOW = blend(BACKGROUND, Color.BLACK, 0.4);
        METAL = blend(MUTED, TEXT, 0.15);
        ICE = Color.decode(switch (palette) {
            case MOONLIGHT -> "#83DFFF";
            case FOREST -> "#9CDDEE";
            case ARCADE -> "#A7B7FF";
        });
    }

    static Color blend(Color a, Color b, double fraction) {
        return new Color((int) (a.getRed() * (1 - fraction) + b.getRed() * fraction),
                (int) (a.getGreen() * (1 - fraction) + b.getGreen() * fraction),
                (int) (a.getBlue() * (1 - fraction) + b.getBlue() * fraction));
    }

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
