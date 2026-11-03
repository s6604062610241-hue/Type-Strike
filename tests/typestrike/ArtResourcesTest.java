package typestrike.ui;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import typestrike.model.ArmoredEnemy;
import typestrike.model.WordRepository;

/** ทดสอบส่วนวาดภาพและไฟล์ที่เกมใช้จริง โดยไม่เปิดหน้าต่างหรือใช้อุปกรณ์เสียง */
public final class ArtResourcesTest {
    private static int[] enemy(int kind, double seconds) {
        BufferedImage image = new BufferedImage(100, 110, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        PixelArt.enemy(g, 25, 30, 3, kind == 1, kind == 1, kind == 2, kind == 3, kind == 4, seconds);
        g.dispose();
        return image.getRGB(0, 0, 100, 110, null, 0, 100);
    }
    public static void main(String[] args) {
        BufferedImage dome = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        var domeCanvas = dome.createGraphics();
        PixelArt.barrier(domeCanvas, 100, 100);
        domeCanvas.dispose();
        if ((dome.getRGB(100, 100) >>> 24) > 64) throw new AssertionError("Dome center must stay clear for wizard");
        if (Arrays.stream(dome.getRGB(0, 0, 200, 200, null, 0, 200)).noneMatch(pixel -> (pixel >>> 24) > 128)) {
            throw new AssertionError("Rune dome must render visible edges");
        }
        var crowns = new HashSet<Integer>();
        for (int rank = 1; rank <= 3; rank++) {
            BufferedImage icon = new BufferedImage(30, 40, BufferedImage.TYPE_INT_ARGB);
            var iconCanvas = icon.createGraphics();
            PixelArt.crown(iconCanvas, rank, 0, 0);
            iconCanvas.dispose();
            int[] pixels = icon.getRGB(0, 0, 30, 40, null, 0, 30);
            if (Arrays.stream(pixels).noneMatch(pixel -> (pixel >>> 24) > 128)) throw new AssertionError("Missing crown " + rank);
            crowns.add(Arrays.hashCode(pixels));
        }
        if (crowns.size() != 3) throw new AssertionError("Rank crowns must have distinct gold/silver/bronze artwork");
        // ทดสอบทำลายและคืนเกราะผ่านกติกาจริง แล้วจึงตรวจภาพที่วาด
        ArmoredEnemy orc = new ArmoredEnemy(6, "armor", 0, 0);
        for (int state = 0; state < 3; state++) {
            if (state == 1) orc.onWordMatched(new WordRepository(), new java.util.Random(5), java.util.Set.of("armor"));
            if (state == 2) orc.restoreArmor();
            BufferedImage rendered = new BufferedImage(100, 110, BufferedImage.TYPE_INT_ARGB);
            var canvas = rendered.createGraphics();
            PixelArt.enemy(canvas, 25, 30, 3, orc.hasArmor(), true, false, false, false, 0);
            canvas.dispose();
            int[] actual = rendered.getRGB(0, 0, 100, 110, null, 0, 100);
            if (!Arrays.equals(actual, enemy(state == 1 ? 0 : 1, 0))) {
                throw new AssertionError("Armor state must select masked/warrior/masked artwork");
            }
        }
        var kinds = new HashSet<Integer>();
        for (int kind = 0; kind < 5; kind++) {
            int[] initial = enemy(kind, 0);
            if (Arrays.stream(initial).noneMatch(pixel -> (pixel >>> 24) != 0)) throw new AssertionError("Empty enemy");
            kinds.add(Arrays.hashCode(initial));
            boolean changes = false;
            for (double time : new double[]{0.18, 0.36, 0.54}) changes |= !Arrays.equals(initial, enemy(kind, time));
            if (!changes) throw new AssertionError("Animation frames do not change for kind " + kind);
        }
        if (kinds.size() != 5) throw new AssertionError("Enemy types must look distinct");
        BufferedImage image = new BufferedImage(1100, 540, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        PixelArt.background(g, 1100, 540, false);
        int[] first = image.getRGB(0, 0, 1100, 540, null, 0, 1100);
        PixelArt.background(g, 1100, 540, true);
        int[] menu = image.getRGB(0, 0, 1100, 540, null, 0, 1100);
        if (Arrays.equals(first, menu)) throw new AssertionError("Menu and arena backgrounds must differ");
        PixelArt.wizard(g, 500, 240, 4, 0.3);
        g.dispose();
        if (Arrays.equals(menu, image.getRGB(0, 0, 1100, 540, null, 0, 1100))) throw new AssertionError("Player not rendered");
        System.out.println("PASS: bundled art loads; five distinct animated enemies, player, menu and arena backgrounds.");
    }
}
