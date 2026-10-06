package client;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.time.LocalTime;

/**
 * Đồng hồ kim tự vẽ bằng Graphics2D — không dùng ảnh có sẵn, để khớp đúng bảng màu
 * "dark flat design" của toàn app. Gọi setTime(LocalTime) mỗi khi có giờ mới để cập nhật.
 */
public class AnalogClockPanel extends JPanel {

    private static final Color FACE_BG   = new Color(0x14, 0x14, 0x22);
    private static final Color RING       = new Color(0x2A, 0x2A, 0x3E);
    private static final Color TICK_MAJOR = new Color(0x5E, 0xEA, 0xD4);
    private static final Color TICK_MINOR = new Color(0x45, 0x45, 0x5E);
    private static final Color HAND_HOUR  = new Color(0xE8, 0xE8, 0xF2);
    private static final Color HAND_MIN   = new Color(0xE8, 0xE8, 0xF2);
    private static final Color HAND_SEC   = new Color(0xA7, 0x8B, 0xFA);
    private static final Color CENTER_DOT = new Color(0xA7, 0x8B, 0xFA);

    private LocalTime time = LocalTime.now();

    public AnalogClockPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(260, 260));
    }

    public void setTime(LocalTime time) {
        this.time = time;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 10;
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        int radius = size / 2;

        // Mặt đồng hồ
        g2.setColor(RING);
        g2.fill(new Ellipse2D.Float(cx - radius, cy - radius, size, size));
        g2.setColor(FACE_BG);
        g2.fill(new Ellipse2D.Float(cx - radius + 6, cy - radius + 6, size - 12, size - 12));

        // 60 vạch chia (mỗi 5 vạch 1 vạch to hơn, đậm màu teal)
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6);
            boolean major = (i % 5 == 0);
            int outer = radius - 8;
            int inner = major ? radius - 18 : radius - 13;
            g2.setColor(major ? TICK_MAJOR : TICK_MINOR);
            g2.setStroke(new BasicStroke(major ? 2.5f : 1.2f));
            int x1 = cx + (int) (outer * Math.sin(angle));
            int y1 = cy - (int) (outer * Math.cos(angle));
            int x2 = cx + (int) (inner * Math.sin(angle));
            int y2 = cy - (int) (inner * Math.cos(angle));
            g2.draw(new Line2D.Float(x1, y1, x2, y2));
        }

        int hour = time.getHour() % 12;
        int minute = time.getMinute();
        int second = time.getSecond();

        drawHand(g2, cx, cy, (hour + minute / 60.0) * 30, radius * 0.5, 5f, HAND_HOUR);
        drawHand(g2, cx, cy, (minute + second / 60.0) * 6, radius * 0.72, 3.5f, HAND_MIN);
        drawHand(g2, cx, cy, second * 6, radius * 0.80, 1.6f, HAND_SEC);

        g2.setColor(CENTER_DOT);
        g2.fill(new Ellipse2D.Float(cx - 6, cy - 6, 12, 12));

        g2.dispose();
    }

    private void drawHand(Graphics2D g2, int cx, int cy, double angleDeg, double length, float width, Color color) {
        Graphics2D h = (Graphics2D) g2.create();
        h.setColor(color);
        h.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        AffineTransform t = new AffineTransform();
        t.translate(cx, cy);
        t.rotate(Math.toRadians(angleDeg));
        h.setTransform(t);
        h.draw(new Line2D.Double(0, 0, 0, -length));
        h.dispose();
    }
}
