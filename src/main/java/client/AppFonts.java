package client;

import java.awt.*;
import java.io.InputStream;

/**
 * Nạp 2 font riêng đóng gói sẵn trong resources (không phụ thuộc font có sẵn trên máy
 * người dùng, nên giao diện lúc nào cũng hiển thị đúng như thiết kế trên MỌI máy Windows):
 *  - "Share Tech Mono": font digital-clock, dùng cho ô hiển thị giờ lớn.
 *  - "Space Grotesk": font sans hiện đại, dùng cho toàn bộ chữ còn lại (tiêu đề, nút bấm...).
 */
public final class AppFonts {

    public static final Font CLOCK_FONT;
    public static final Font UI_FONT;

    static {
        Font clockBase = loadFont("/fonts/ShareTechMono-Regular.ttf");
        Font uiBase = loadFont("/fonts/SpaceGrotesk.ttf");
        CLOCK_FONT = clockBase != null ? clockBase : new Font(Font.MONOSPACED, Font.PLAIN, 12);
        UI_FONT = uiBase != null ? uiBase : new Font("Segoe UI", Font.PLAIN, 12);
    }

    private AppFonts() {}

    private static Font loadFont(String resourcePath) {
        try (InputStream is = AppFonts.class.getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            Font font = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            System.err.println("Khong nap duoc font " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    public static Font clock(float size) { return CLOCK_FONT.deriveFont(size); }
    public static Font ui(int style, float size) { return UI_FONT.deriveFont(style, size); }
}
