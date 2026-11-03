package typestrike;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import typestrike.model.GameModel;
import typestrike.model.WordRepository;
import typestrike.ui.GameWindow;
import typestrike.ui.PlayScreen;

/** ทดสอบการทำงานร่วมกันของหน้าจอ Swing และวาดภาพตรวจรูปแบบ โดยไม่เปิดหน้าต่างจริง */
public final class UiSmokeTest {
    private static JButton button(Container root, String prefix) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && b.getText().startsWith(prefix)) return b;
            if (c instanceof Container nested) {
                JButton found = button(nested, prefix);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JTextField field(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextField f) return f;
            if (c instanceof Container nested) {
                JTextField found = field(nested);
                if (found != null) return found;
            }
        }
        return null;
    }

    static void capture(GameWindow window, String name) throws Exception {
        window.validate();
        Container content = window.getContentPane();
        BufferedImage image = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        content.printAll(g);
        g.dispose();
        ImageIO.write(image, "png", Path.of("build", "ui-review", name + ".png").toFile());
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build", "ui-review"));
        final Throwable[] problem = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            GameWindow window = null;
            try {
                window = new GameWindow();
                window.addNotify(); // เตรียมส่วนที่หน้าต่างต้องใช้จัดวาง โดยไม่แสดงหรือเลือกหน้าต่าง
                capture(window, "menu");
                button((Container) window.getContentPane().getComponent(0), "START").doClick();
                PlayScreen play = (PlayScreen) window.getContentPane().getComponent(1);
                play.onHide(); // หยุดตัวจับเวลาจริง แล้วให้ชุดทดสอบกำหนดเวลาเกมเอง
                GameModel model = new GameModel(new WordRepository(), new Random(73));
                for (int i = 0; i < 130; i++) model.update(0.1);
                play.setModel(model);
                capture(window, "gameplay");
                button(play, "PAUSE").doClick();
                if (!model.isPaused()) throw new AssertionError("Pause button did not pause");
                capture(window, "pause");
                button(play, "RESUME").doClick();
                if (model.isPaused()) throw new AssertionError("Resume button did not resume");
                JTextField input = field(play);
                input.setText(model.getEnemies().get(0).getWord());
                input.postActionEvent();
                if (model.getScore() != 10 || !input.getText().isEmpty()) throw new AssertionError("Enter event did not score and clear input");
                for (int i = 0; i < 20000 && !model.isGameOver(); i++) model.update(0.1);
                window.finishGame(model);
                capture(window, "gameover");
                button((Container) window.getContentPane().getComponent(2), "PLAY AGAIN").doClick();
                play.onHide();
                window.showMenu();
                window.setSize(960, 720);
                capture(window, "menu-minimum");
                System.out.println("PASS: Swing Start, Pause, Resume, Enter, Game Over, Play Again; five layout renders.");
            } catch (Throwable e) { problem[0] = e; }
            finally { if (window != null) window.dispose(); }
        });
        if (problem[0] != null) {
            problem[0].printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
