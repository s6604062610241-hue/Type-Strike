package typestrike.ui;

import java.awt.Color;
import java.awt.Graphics2D;

/** Original code-drawn placeholder sprites. No downloaded art or font dependencies. */
final class PixelArt {
    private static final String[] WIZARD = {
        "......pp........", ".....pppp.......", "....pppppp......", "...pppppppp.....",
        "..pppppppppp....", ".....ssss.......", ".....swsw.......", ".....ssss.......",
        "....pppppp......", "...pppppppp..g..", "..pppppppppp.g..", "...pppppppp..g..",
        "...pppppppp..g..", "..pppppppppp.g..", "....pp..pp...g..", "...dd....dd..g.."
    };
    private static final String[] SLIME = {
        "................", "......rrrr......", "....rrrrrrrr....", "...rrrrrrrrrr...",
        "..rrrrrrrrrrrr..", ".rrrrrrrrrrrrrr.", ".rrrwwrrrrwwrrr.", ".rrrddrrrrddrrr.",
        ".rrrrrrrrrrrrrr.", ".rrrrrddddrrrrr.", ".rrrrrrrrrrrrrr.", "..rrrrrrrrrrrr..",
        "...rrrrrrrrrr...", "..rrr..rr..rrr..", "................", "................"
    };

    private PixelArt() { }

    static void wizard(Graphics2D g, int x, int y, int scale) { draw(g, WIZARD, x, y, scale, Theme.PURPLE); }

    static void enemy(Graphics2D g, int x, int y, int scale, boolean armor, boolean elite) {
        enemy(g, x, y, scale, armor, elite, false);
    }

    static void enemy(Graphics2D g, int x, int y, int scale, boolean armor, boolean elite, boolean frost) {
        draw(g, SLIME, x, y, scale, frost ? Theme.ICE : elite ? Theme.GOLD : Theme.RED);
        if (frost) {
            g.setColor(Theme.TEXT);
            g.fillRect(x + 7 * scale, y - 2 * scale, 2 * scale, 2 * scale);
            g.fillRect(x + 6 * scale, y, 4 * scale, 2 * scale);
            g.fillRect(x + 7 * scale, y + 2 * scale, 2 * scale, 2 * scale);
        }
        if (armor) {
            g.setColor(Theme.METAL);
            g.fillRect(x + 3 * scale, y + 2 * scale, 10 * scale, 3 * scale);
            g.fillRect(x + 2 * scale, y + 4 * scale, 2 * scale, 5 * scale);
            g.fillRect(x + 12 * scale, y + 4 * scale, 2 * scale, 5 * scale);
            g.setColor(Theme.TEXT);
            g.fillRect(x + 6 * scale, y + 2 * scale, 4 * scale, scale);
        }
    }

    private static void draw(Graphics2D g, String[] rows, int x, int y, int scale, Color body) {
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                char pixel = rows[row].charAt(col);
                Color color = switch (pixel) {
                    case 'p', 'r' -> body;
                    case 's' -> new Color(255, 213, 167);
                    case 'w' -> Theme.TEXT;
                    case 'd' -> Theme.SHADOW;
                    case 'g' -> Theme.GREEN;
                    default -> null;
                };
                if (color != null) {
                    g.setColor(color);
                    g.fillRect(x + col * scale, y + row * scale, scale, scale);
                }
            }
        }
    }
}
