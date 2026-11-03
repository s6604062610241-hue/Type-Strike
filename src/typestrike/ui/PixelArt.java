package typestrike.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** โหลดภาพตัวละครและฉากที่รวมไว้ในเกม แต่ละโฟลเดอร์มีเครดิตแหล่งที่มา */
final class PixelArt {
    private static final BufferedImage BARRIER = fittedSprite("/art/custom/rune-dome.png", 144, 144);
    private static final BufferedImage[] CROWNS = {
        fittedSprite("/art/custom/crown-gold.png", 30, 25),
        fittedSprite("/art/custom/crown-silver.png", 30, 25),
        fittedSprite("/art/custom/crown-bronze.png", 30, 25)
    };
    private static final BufferedImage[] PLAYER = frames("wizzard_m_idle_anim_f");
    private static final BufferedImage[][] ENEMIES = {
        frames("orc_warrior_run_anim_f"), frames("masked_orc_run_anim_f"),
        frames("ice_zombie_anim_f"), frames("imp_run_anim_f"), frames("orc_shaman_run_anim_f")
    };
    private static final Rectangle[] ENEMY_BOUNDS = {
        bounds(ENEMIES[0]), bounds(ENEMIES[1]), bounds(ENEMIES[2]), bounds(ENEMIES[3]), bounds(ENEMIES[4])
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
        return loadResource("/art/0x72/" + name);
    }
    private static BufferedImage loadResource(String name) {
        try (var stream = PixelArt.class.getResourceAsStream(name)) {
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
    private static int frame(double seconds) { return Math.floorMod((int) Math.floor(seconds * 6), 4); }
    private static Rectangle bounds(BufferedImage[] frames) {
        return bounds(frames, 0);
    }
    private static Rectangle bounds(BufferedImage[] frames, int alphaThreshold) {
        Rectangle bounds = null;
        for (BufferedImage image : frames) for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) if ((image.getRGB(x, y) >>> 24) > alphaThreshold) {
                if (bounds == null) bounds = new Rectangle(x, y, 1, 1);
                else bounds.add(new Rectangle(x, y, 1, 1));
            }
        }
        if (bounds == null) throw new IllegalStateException("Empty sprite animation");
        return bounds;
    }
    /** ตัดขอบโปร่งใสและเก็บภาพขนาดพร้อมใช้ไว้ครั้งเดียว ไม่ทำใหม่ทุกครั้งที่วาด */
    private static BufferedImage fittedSprite(String resource, int maxWidth, int maxHeight) {
        BufferedImage original = loadResource(resource);
        Rectangle region = bounds(new BufferedImage[]{original}, 32);
        double factor = Math.min(maxWidth / (double) region.width, maxHeight / (double) region.height);
        int width = Math.max(1, (int) Math.round(region.width * factor));
        int height = Math.max(1, (int) Math.round(region.height * factor));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(original, 0, 0, width, height, region.x, region.y,
                region.x + region.width, region.y + region.height, null);
        g.dispose();
        return result;
    }
    private static void draw(Graphics2D g, BufferedImage image, int x, int y, int scale) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, x, y, image.getWidth() * scale, image.getHeight() * scale, null);
    }

    static void wizard(Graphics2D g, int x, int y, int scale) { wizard(g, x, y, scale, 0); }
    static void crown(Graphics2D g, int rank, int x, int y) {
        BufferedImage crown = CROWNS[rank - 1];
        g.drawImage(crown, x + (30 - crown.getWidth()) / 2, y + (40 - crown.getHeight()) / 2, null);
    }
    static void barrier(Graphics2D g, int centerX, int centerY) {
        g.drawImage(BARRIER, centerX - BARRIER.getWidth() / 2, centerY - BARRIER.getHeight() / 2, null);
    }
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
        // ออร์คถอดหน้ากากเมื่อเกราะแตก และกลับมาใส่หน้ากากเมื่อได้รับเกราะคืน
        int kind = healer ? 4 : blast ? 3 : frost ? 2 : elite && armor ? 1 : 0;
        BufferedImage image = ENEMIES[kind][frame(seconds)];
        Rectangle region = ENEMY_BOUNDS[kind];
        // ปรับขนาดตามความสูงตัวละครที่มองเห็น โดยไม่นับขอบโปร่งใสและคงสัดส่วนเดิม
        int actual = Math.max(1, Math.round(66f * scale / 3 / region.height));
        int left = x + (16 * scale - region.width * actual) / 2;
        int top = y + (16 * scale - region.height * actual) / 2;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, left, top, left + region.width * actual, top + region.height * actual,
                region.x, region.y, region.x + region.width, region.y + region.height, null);
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
