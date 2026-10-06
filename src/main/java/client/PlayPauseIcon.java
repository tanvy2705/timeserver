package client;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Icon Pause (2 thanh dọc) / Resume (tam giác play) TỰ VẼ bằng Graphics2D.
 * Không dùng ký tự Unicode (❚❚, ▶) vì nhiều font Windows mặc định không có sẵn 2 ký tự
 * đặc biệt này, dễ hiện thành ô vuông trống (□□) trên máy người dùng.
 */
public class PlayPauseIcon implements Icon {

    public enum Kind { PAUSE, PLAY }

    private final Kind kind;
    private final Color color;
    private final int size;

    public PlayPauseIcon(Kind kind, Color color, int size) {
        this.kind = kind;
        this.color = color;
        this.size = size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);

        if (kind == Kind.PAUSE) {
            int barWidth = size / 3;
            int gap = size / 4;
            g2.fill(new RoundRectangle2D.Float(x, y, barWidth, size, 2, 2));
            g2.fill(new RoundRectangle2D.Float(x + barWidth + gap, y, barWidth, size, 2, 2));
        } else {
            Path2D.Float triangle = new Path2D.Float();
            triangle.moveTo(x, y);
            triangle.lineTo(x, y + size);
            triangle.lineTo(x + size, y + size / 2f);
            triangle.closePath();
            g2.fill(triangle);
        }
        g2.dispose();
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
}
