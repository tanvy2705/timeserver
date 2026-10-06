package client;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Nút bấm tự vẽ lại hoàn toàn (không dùng giao diện Swing mặc định vuông vức, xám xịt)
 * để có phong cách "flat design" bo góc, đổi màu theo trạng thái hover/disabled.
 */
public class RoundedButton extends JButton {

    private Color baseColor;
    private final Color hoverColor;
    private final Color textColor;

    public RoundedButton(String text, Color baseColor, Color hoverColor, Color textColor) {
        super(text);
        this.baseColor = baseColor;
        this.hoverColor = hoverColor;
        this.textColor = textColor;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(textColor);
        setFont(new Font("Segoe UI", Font.BOLD, 15));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(10, 22, 10, 22));
    }

    public void setBaseColor(Color c) { this.baseColor = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill;
        if (!isEnabled()) {
            fill = new Color(70, 70, 85);
        } else if (getModel().isRollover()) {
            fill = hoverColor;
        } else {
            fill = baseColor;
        }
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 18, 18));
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public boolean contains(int x, int y) {
        return new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18).contains(x, y);
    }
}
