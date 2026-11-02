package typestrike.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Bundled 0x72 CC0 art. Resource loading and palette variants are cached once. */
final class PixelArt {
    private static final BufferedImage[] PLAYER = frames("wizzard_m_idle_anim_f");
    private static final BufferedImage[] SMALL = frames("tiny_slug_anim_f");
    private static final BufferedImage[] LARGE = frames("slug_anim_f");
    private static final BufferedImage[][] ENEMIES = {
        SMALL, tint(LARGE, Theme.GOLD), tint(SMALL, Theme.ICE),
        tint(SMALL, Theme.PURPLE), tint(SMALL, Theme.GREEN)
    };
    private static final Rectangle[] ENEMY_BOUNDS = {
        bounds(SMALL), bounds(LARGE), bounds(SMALL), bounds(SMALL), bounds(SMALL)
    };
    private static final BufferedImage[] FLOORS = {
        load("floor_1.png"), load("floor_2.png"), load("floor_3.png"), load("floor_5.png")
    };
    private static final BufferedImage WALL = load("wall_mid.png");
    private static final BufferedImage WALL_CAP = load("wall_top_mid.png");
    private static final BufferedImage COLUMN = load("column.png");
    private static final BufferedImage BLUE_BANNER = load("wall_banner_blue.png");
    private static final BufferedImage RED_BANNER = load("wall_banner_red.png");
    private static BufferedImage menuBackdrop;
    private static BufferedImage fieldBackdrop;

    private PixelArt() { }

    private static BufferedImage load(String name) {
        try (var stream = PixelArt.class.getResourceAsStream("/art/0x72/" + name)) {
            if (stream == null) throw new IllegalStateException("Missing art: " + name);
            BufferedImage result = ImageIO.read(stream);
            if (result == null) throw new IllegalStateException("Invalid PNG: " + name);
            return result;
        } catch (IOException failure) { throw new IllegalStateException("Cannot read art: " + name, failure); }
    }
    private static BufferedImage[] frames(String prefix) {
        BufferedImage[] result = new BufferedImage[4];
        for (int i = 0; i < result.length; i++) result[i] = load(prefix + i + ".png");
        return result;
    }
    /** Runtime palette swap preserves original shading, alpha and neutral eye pixels. */
    private static BufferedImage[] tint(BufferedImage[] original, Color color) {
        float hue = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
        BufferedImage[] result = new BufferedImage[original.length];
        for (int i = 0; i < original.length; i++) {
            BufferedImage source = original[i];
            BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                float[] hsb = Color.RGBtoHSB((pixel >> 16) & 255, (pixel >> 8) & 255, pixel & 255, null);
                if ((pixel >>> 24) != 0 && hsb[1] > 0.25f) {
                    pixel = (pixel & 0xff000000) | (Color.HSBtoRGB(hue, hsb[1] * 0.8f, hsb[2]) & 0xffffff);
                }
                image.setRGB(x, y, pixel);
            }
            result[i] = image;
        }
        return result;
    }
    private static int frame(double seconds) { return Math.floorMod((int) Math.floor(seconds * 6), 4); }
    private static Rectangle bounds(BufferedImage[] frames) {
        Rectangle bounds = null;
        for (BufferedImage image : frames) for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) >>> 24) != 0) {
                if (bounds == null) bounds = new Rectangle(x, y, 1, 1);
                else bounds.add(new Rectangle(x, y, 1, 1));
            }
        }
        if (bounds == null) throw new IllegalStateException("Empty sprite animation");
        return bounds;
    }
    private static void draw(Graphics2D g, BufferedImage image, int x, int y, int scale) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, x, y, image.getWidth() * scale, image.getHeight() * scale, null);
    }

    static void wizard(Graphics2D g, int x, int y, int scale) { wizard(g, x, y, scale, 0); }
    static void wizard(Graphics2D g, int x, int y, int scale, double seconds) {
        BufferedImage image = PLAYER[frame(seconds)];
        int actual = Math.max(1, scale - 1);
        draw(g, image, x + (16 * scale - image.getWidth() * actual) / 2,
                y + (16 * scale - image.getHeight() * actual) / 2, actual);
    }
    static void enemy(Graphics2D g, int x, int y, int scale, boolean armor, boolean elite) {
        enemy(g, x, y, scale, armor, elite, false);
    }
    static void enemy(Graphics2D g, int x, int y, int scale, boolean armor, boolean elite, boolean frost) {
        enemy(g, x, y, scale, armor, elite, frost, false, false);
    }
    static void enemy(Graphics2D g, int x, int y, int scale, boolean armor, boolean elite,
                      boolean frost, boolean blast, boolean healer) {
        enemy(g, x, y, scale, armor, elite, frost, blast, healer, 0);
    }
    static void enemy(Graphics2D g, int x, int y, int scale, boolean armor, boolean elite,
                      boolean frost, boolean blast, boolean healer, double seconds) {
        int kind = healer ? 4 : blast ? 3 : frost ? 2 : elite ? 1 : 0;
        BufferedImage image = ENEMIES[kind][frame(seconds)];
        Rectangle region = ENEMY_BOUNDS[kind];
        int actual = Math.max(1, Math.min(16 * scale / region.width, 16 * scale / region.height));
        int left = x + (16 * scale - region.width * actual) / 2;
        int top = y + (16 * scale - region.height * actual) / 2;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, left, top, left + region.width * actual, top + region.height * actual,
                region.x, region.y, region.x + region.width, region.y + region.height, null);
        y = top; // Markers attach to the visible body, not transparent padding.
        int cx = x + 8 * scale;
        if (frost) {
            g.setColor(Theme.ICE);
            g.fillPolygon(new int[]{cx, cx + 2 * scale, cx, cx - 2 * scale},
                    new int[]{y - 3 * scale, y, y + 3 * scale, y}, 4);
        }
        if (blast) {
            g.setColor(Theme.TEXT); g.fillRect(cx - scale, y - 2 * scale, scale * 2, scale * 3);
            g.setColor(Theme.GOLD); g.fillRect(cx + scale, y - 3 * scale, scale * 2, scale * 2);
        }
        if (healer) {
            g.setColor(Theme.GREEN);
            g.fillRect(cx - 2 * scale, y - scale, 4 * scale, scale);
            g.fillRect(cx - scale, y - 2 * scale, 2 * scale, 3 * scale);
        }
        if (armor) {
            g.setColor(Theme.METAL);
            g.fillRect(cx - 4 * scale, y + scale, 8 * scale, 3 * scale);
            g.setColor(Theme.TEXT); g.fillRect(cx - scale, y + scale, 2 * scale, scale);
        }
    }

    static void background(Graphics2D g, int width, int height, boolean menu) {
        if (width <= 0 || height <= 0) return;
        BufferedImage cached = menu ? menuBackdrop : fieldBackdrop;
        if (cached == null || cached.getWidth() != width || cached.getHeight() != height) {
            cached = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D canvas = cached.createGraphics();
            final int tile = 48;
            for (int row = 0; row * tile < height; row++) for (int col = 0; col * tile < width; col++) {
                int variation = Math.floorMod(col * 17 + row * 31, 13);
                BufferedImage floor = FLOORS[variation < 10 ? 0 : variation - 9];
                draw(canvas, floor, col * tile, row * tile, 3);
            }
            for (int x = 0; x < width; x += tile) {
                draw(canvas, WALL, x, 0, 3);
                draw(canvas, WALL_CAP, x, height - tile, 3);
            }
            if (menu) {
                draw(canvas, COLUMN, 48, 30, 3);
                draw(canvas, COLUMN, width - 96, 30, 3);
                draw(canvas, BLUE_BANNER, 144, 0, 3);
                draw(canvas, RED_BANNER, width - 192, 0, 3);
            }
            canvas.setColor(new Color(8, 13, 24, menu ? 185 : 155));
            canvas.fillRect(0, 0, width, height);
            canvas.dispose();
            if (menu) menuBackdrop = cached; else fieldBackdrop = cached;
        }
        g.drawImage(cached, 0, 0, null);
    }
}
