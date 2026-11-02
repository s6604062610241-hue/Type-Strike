package typestrike.ui;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;

/** Exercises the real renderer/resources without Swing windows or an audio device. */
public final class ArtResourcesTest {
    private static int[] enemy(int kind, double seconds) {
        BufferedImage image = new BufferedImage(100, 110, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        PixelArt.enemy(g, 25, 30, 3, kind == 1, kind == 1, kind == 2, kind == 3, kind == 4, seconds);
        g.dispose();
        return image.getRGB(0, 0, 100, 110, null, 0, 100);
    }
    public static void main(String[] args) {
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
