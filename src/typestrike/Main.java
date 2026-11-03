package typestrike;

import javax.swing.SwingUtilities;
import typestrike.ui.GameWindow;

/** จุดเริ่มโปรแกรม สร้างส่วนของหน้าจอ Swing บนเธรดที่ดูแลหน้าจอ */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().setVisible(true));
    }
}
