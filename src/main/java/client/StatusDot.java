package client;

import javax.swing.*;
import java.awt.*;

/** 1 chấm tròn nhỏ đổi màu theo trạng thái — thay cho việc phải dùng ảnh icon có sẵn. */
public class StatusDot implements Icon {

    private Color color;
    private final int size;

    public StatusDot(Color color, int size) {
        this.color = color;
        this.size = size;
    }

    public void setColor(Color color) { this.color = color; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // vòng phát sáng mờ phía ngoài cho hiệu ứng "đèn tín hiệu"
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g2.fillOval(x, y, size, size);
        g2.setColor(color);
        int inset = size / 4;
        g2.fillOval(x + inset, y + inset, size - 2 * inset, size - 2 * inset);
        g2.dispose();
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
}
