package typestrike.ui;

import javax.swing.JPanel;

/** กำหนดเมท็อดร่วมสำหรับเข้าและออกจากหน้า ให้แต่ละหน้าจอเขียนการทำงานของตัวเอง */
public abstract class GameScreen extends JPanel {
    protected final GameWindow window;

    @SuppressWarnings("this-escape") // เรียกเมท็อดตั้งค่าของ Swing ได้ก่อนแสดงหน้าจอเหล่านี้
    protected GameScreen(GameWindow window) {
        this.window = window;
        setBackground(Theme.BACKGROUND);
    }

    public abstract void onShow();
    public void onHide() { }
}
