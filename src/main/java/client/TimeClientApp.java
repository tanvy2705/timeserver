package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Toàn bộ GUI Client trong 1 cửa sổ duy nhất, chuyển màn hình bằng CardLayout:
 *  - "connect": nhập IP/Port Server, bấm Kết nối.
 *  - "clock"  : hiển thị giờ realtime (2 kiểu: số điện tử / kim đồng hồ) + Pause/Resume.
 *
 * HÀNH VI KHI MẤT SERVER (đã sửa theo yêu cầu): đồng hồ KHÔNG đứng khựng lại, mà tự
 * chuyển sang chạy bằng giờ hệ thống của chính máy Client (fallbackTimer), có nhãn rõ
 * "Ngoai tuyen". Pause/Resume bị khoá (không còn Server để gửi lệnh tới). Có nút
 * "Ket noi lai" để quay về màn hình đăng nhập, thử kết nối lại (Server cũ hoặc IP khác).
 */
public class TimeClientApp extends JFrame {

    // ------- Bảng màu "dark flat design" dùng xuyên suốt -------
    private static final Color BG            = new Color(0x0F, 0x0F, 0x18);
    private static final Color CARD_BG       = new Color(0x1A, 0x1A, 0x28);
    private static final Color CARD_BG_2     = new Color(0x21, 0x21, 0x33);
    private static final Color TEXT_PRIMARY  = new Color(0xE8, 0xE8, 0xF2);
    private static final Color TEXT_MUTED    = new Color(0x8A, 0x8A, 0xA3);
    private static final Color ACCENT_TEAL   = new Color(0x5E, 0xEA, 0xD4);
    private static final Color ACCENT_PURPLE = new Color(0xA7, 0x8B, 0xFA);
    private static final Color GREEN_RUN     = new Color(0x4A, 0xDE, 0x80);
    private static final Color AMBER_PAUSE   = new Color(0xFB, 0xBF, 0x24);
    private static final Color RED_OFFLINE   = new Color(0xF8, 0x71, 0x71);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardHolder = new JPanel(cardLayout);

    // ----- Thành phần màn hình đồng hồ -----
    private final CardLayout clockFaceLayout = new CardLayout();
    private final JPanel clockFaceHolder = new JPanel(clockFaceLayout);
    private JLabel lblDigital;
    private AnalogClockPanel analogClock;
    private boolean analogMode = false;

    private JLabel lblDate;
    private JLabel lblStatusText;
    private StatusDot statusDot;
    private JLabel lblStatusIcon;
    private RoundedButton btnPause;
    private RoundedButton btnResume;
    private RoundedButton btnReconnect;
    private JLabel lblServerInfo;

    private Socket socket;
    private PrintWriter out;
    private volatile boolean connected = false;
    private javax.swing.Timer fallbackTimer; // chạy giờ máy khi mất Server

    public TimeClientApp() {
        super("TCP Time Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 620);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        cardHolder.setOpaque(false);
        cardHolder.add(buildConnectScreen(), "connect");
        cardHolder.add(buildClockScreen(), "clock");
        add(cardHolder);

        cardLayout.show(cardHolder, "connect");
    }

    // ================= MÀN HÌNH KẾT NỐI =================

    private JComponent buildConnectScreen() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        RoundedPanel card = new RoundedPanel(CARD_BG, 24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 40, 36, 40));
        card.setPreferredSize(new Dimension(380, 300));

        JLabel title = new JLabel("TCP TIME CLIENT");
        title.setFont(AppFonts.ui(Font.BOLD, 22));
        title.setForeground(ACCENT_TEAL);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Ket noi toi Time Server tren mang LAN");
        subtitle.setFont(AppFonts.ui(Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField txtHost = styledField("127.0.0.1");
        JTextField txtPort = styledField(String.valueOf(server.TimeServer.PORT));

        JLabel lblHost = fieldLabel("Dia chi IP Server:");
        JLabel lblPort = fieldLabel("Port:");

        RoundedButton btnConnect = new RoundedButton("KET NOI  →", ACCENT_TEAL, ACCENT_TEAL.brighter(), BG);
        btnConnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConnect.setMaximumSize(new Dimension(260, 46));

        JLabel lblError = new JLabel(" ");
        lblError.setFont(AppFonts.ui(Font.PLAIN, 12));
        lblError.setForeground(RED_OFFLINE);
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnConnect.addActionListener(e -> {
            lblError.setText(" ");
            String host = txtHost.getText().trim();
            String portStr = txtPort.getText().trim();
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                lblError.setText("Port khong hop le");
                return;
            }
            btnConnect.setEnabled(false);
            btnConnect.setText("DANG KET NOI...");
            connectToServer(host, port, lblError, btnConnect);
        });

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(28));
        card.add(lblHost);
        card.add(Box.createVerticalStrut(6));
        card.add(txtHost);
        card.add(Box.createVerticalStrut(16));
        card.add(lblPort);
        card.add(Box.createVerticalStrut(6));
        card.add(txtPort);
        card.add(Box.createVerticalStrut(24));
        card.add(btnConnect);
        card.add(Box.createVerticalStrut(10));
        card.add(lblError);

        wrapper.add(card);
        return wrapper;
    }

    private JTextField styledField(String defaultText) {
        JTextField field = new JTextField(defaultText);
        field.setFont(AppFonts.ui(Font.PLAIN, 15));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(new Color(0x24, 0x24, 0x36));
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        field.setMaximumSize(new Dimension(300, 40));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        return field;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppFonts.ui(Font.PLAIN, 12));
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    // ================= MÀN HÌNH ĐỒNG HỒ =================

    private JComponent buildClockScreen() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 30, 30, 30));

        lblServerInfo = new JLabel("", SwingConstants.CENTER);
        lblServerInfo.setFont(AppFonts.ui(Font.PLAIN, 12));
        lblServerInfo.setForeground(TEXT_MUTED);
        root.add(lblServerInfo, BorderLayout.NORTH);

        // ----- Thẻ đồng hồ chính -----
        RoundedPanel clockCard = new RoundedPanel(CARD_BG, 28);
        clockCard.setLayout(new BoxLayout(clockCard, BoxLayout.Y_AXIS));
        clockCard.setBorder(BorderFactory.createEmptyBorder(32, 20, 32, 20));

        lblDate = new JLabel("", SwingConstants.CENTER);
        lblDate.setFont(AppFonts.ui(Font.PLAIN, 13));
        lblDate.setForeground(TEXT_MUTED);
        lblDate.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ----- 2 kiểu mặt đồng hồ, chuyển qua lại bằng CardLayout riêng -----
        lblDigital = new JLabel("--:--:--", SwingConstants.CENTER);
        lblDigital.setFont(AppFonts.clock(58f));
        lblDigital.setForeground(ACCENT_TEAL);

        analogClock = new AnalogClockPanel();
        JPanel analogWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        analogWrap.setOpaque(false);
        analogWrap.add(analogClock);

        clockFaceHolder.setOpaque(false);
        clockFaceHolder.add(lblDigital, "digital");
        clockFaceHolder.add(analogWrap, "analog");
        clockFaceHolder.setAlignmentX(Component.CENTER_ALIGNMENT);
        clockFaceLayout.show(clockFaceHolder, "digital");

        // ----- Nút chuyển kiểu hiển thị (segmented toggle) -----
        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        toggleRow.setOpaque(false);
        toggleRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        RoundedPanel toggleTrack = new RoundedPanel(CARD_BG_2, 16);
        toggleTrack.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 4));
        JToggleButton btnDigitalMode = segmentButton("SO", true);
        JToggleButton btnAnalogMode = segmentButton("KIM", false);
        ButtonGroup group = new ButtonGroup();
        group.add(btnDigitalMode);
        group.add(btnAnalogMode);
        btnDigitalMode.addActionListener(e -> switchClockFace(false));
        btnAnalogMode.addActionListener(e -> switchClockFace(true));
        toggleTrack.add(btnDigitalMode);
        toggleTrack.add(btnAnalogMode);
        toggleRow.add(toggleTrack);

        statusDot = new StatusDot(RED_OFFLINE, 14);
        lblStatusIcon = new JLabel(statusDot);
        lblStatusText = new JLabel("Mat ket noi");
        lblStatusText.setFont(AppFonts.ui(Font.PLAIN, 13));
        lblStatusText.setForeground(TEXT_MUTED);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        statusRow.setOpaque(false);
        statusRow.add(lblStatusIcon);
        statusRow.add(lblStatusText);
        statusRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        clockCard.add(lblDate);
        clockCard.add(Box.createVerticalStrut(10));
        clockCard.add(clockFaceHolder);
        clockCard.add(Box.createVerticalStrut(18));
        clockCard.add(toggleRow);
        clockCard.add(Box.createVerticalStrut(14));
        clockCard.add(statusRow);

        root.add(clockCard, BorderLayout.CENTER);

        // ----- Hàng nút điều khiển -----
        btnPause = new RoundedButton("PAUSE", AMBER_PAUSE, AMBER_PAUSE.brighter(), BG);
        btnResume = new RoundedButton("RESUME", GREEN_RUN, GREEN_RUN.brighter(), BG);
        btnReconnect = new RoundedButton("KET NOI LAI", ACCENT_PURPLE, ACCENT_PURPLE.brighter(), BG);
        btnPause.setIcon(new PlayPauseIcon(PlayPauseIcon.Kind.PAUSE, BG, 15));
        btnResume.setIcon(new PlayPauseIcon(PlayPauseIcon.Kind.PLAY, BG, 15));
        btnPause.setIconTextGap(10);
        btnResume.setIconTextGap(10);
        btnResume.setEnabled(false);
        btnReconnect.setVisible(false); // chỉ hiện khi mất kết nối

        btnPause.addActionListener(e -> sendCommand("PAUSE", true));
        btnResume.addActionListener(e -> sendCommand("RESUME", false));
        btnReconnect.addActionListener(e -> goBackToConnectScreen());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttonRow.setOpaque(false);
        buttonRow.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        buttonRow.add(btnPause);
        buttonRow.add(btnResume);
        buttonRow.add(btnReconnect);
        root.add(buttonRow, BorderLayout.SOUTH);

        return root;
    }

    private JToggleButton segmentButton(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected);
        btn.setFont(AppFonts.ui(Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(6, 18, 6, 18));
        applySegmentColors(btn);
        btn.addChangeListener(e -> applySegmentColors(btn));
        return btn;
    }

    private void applySegmentColors(JToggleButton btn) {
        if (btn.isSelected()) {
            btn.setBackground(ACCENT_TEAL);
            btn.setForeground(BG);
        } else {
            btn.setBackground(CARD_BG_2);
            btn.setForeground(TEXT_MUTED);
        }
        btn.setOpaque(true);
    }

    private void switchClockFace(boolean toAnalog) {
        this.analogMode = toAnalog;
        clockFaceLayout.show(clockFaceHolder, toAnalog ? "analog" : "digital");
    }

    // ================= MẠNG =================

    private static final int CONNECT_TIMEOUT_MS = 5000; // 5 giây, tránh treo vô thời hạn

    private void connectToServer(String host, int port, JLabel lblError, RoundedButton btnConnect) {
        new Thread(() -> {
            try {
                // Dùng connect(SocketAddress, timeout) thay vì new Socket(host, port):
                // nếu Firewall máy Server ÂM THẦM chặn (không trả lời gì cả, khác với
                // "từ chối" hẳn), cách gọi mặc định có thể treo tới VÀI PHÚT trước khi
                // báo lỗi — rất khó hiểu với người mới test LAN. Đặt timeout rõ ràng
                // 5 giây để báo lỗi nhanh, kèm gợi ý đúng nguyên nhân thường gặp.
                Socket s = new Socket();
                s.connect(new java.net.InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                s.setTcpNoDelay(true); // giảm độ trễ hiển thị, không gộp gói tin khi gửi PAUSE/RESUME
                this.socket = s;
                this.out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
                connected = true;
                stopFallbackTimer();

                SwingUtilities.invokeLater(() -> {
                    lblServerInfo.setText("Da ket noi toi " + host + ":" + port);
                    setStatus(true, false);
                    btnReconnect.setVisible(false);
                    cardLayout.show(cardHolder, "clock");
                    btnConnect.setEnabled(true);
                    btnConnect.setText("KET NOI  →");
                });

                readTimeLoop(new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8")));
            } catch (java.net.SocketTimeoutException e) {
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    lblError.setText("Het thoi gian cho (5s) — kiem tra lai Firewall may Server");
                    btnConnect.setEnabled(true);
                    btnConnect.setText("KET NOI  →");
                });
            } catch (java.net.ConnectException e) {
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    lblError.setText("Server tu choi ket noi — IP dung nhung Server chua chay?");
                    btnConnect.setEnabled(true);
                    btnConnect.setText("KET NOI  →");
                });
            } catch (java.net.UnknownHostException e) {
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    lblError.setText("Khong tim thay dia chi IP nay — kiem tra lai da go dung chua");
                    btnConnect.setEnabled(true);
                    btnConnect.setText("KET NOI  →");
                });
            } catch (IOException e) {
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    lblError.setText("Khong ket noi duoc: " + e.getMessage());
                    btnConnect.setEnabled(true);
                    btnConnect.setText("KET NOI  →");
                });
            }
        }, "server-reader").start();
    }

    /** Chạy trên Thread nền: đọc liên tục các dòng giờ Server gửi xuống, cập nhật GUI. */
    private void readTimeLoop(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String timeStr = line;
                SwingUtilities.invokeLater(() -> updateClockDisplay(timeStr));
            }
        } catch (IOException ignored) {
            // Server tắt hoặc mất mạng giữa chừng
        } finally {
            connected = false;
            SwingUtilities.invokeLater(this::handleDisconnected);
        }
    }

    /** Cập nhật cả 2 mặt đồng hồ (số điện tử + kim) cùng lúc từ 1 chuỗi giờ HH:mm:ss. */
    private void updateClockDisplay(String timeStr) {
        lblDigital.setText(timeStr);
        try {
            analogClock.setTime(LocalTime.parse(timeStr, TIME_FMT));
        } catch (Exception ignored) {}
    }

    private void handleDisconnected() {
        setStatus(false, false);
        lblStatusText.setText("Ngoai tuyen - hien gio may");
        btnPause.setEnabled(false);
        btnResume.setEnabled(false);
        btnReconnect.setVisible(true);
        // Đồng hồ KHÔNG đứng khựng — chuyển sang chạy bằng giờ hệ thống của máy Client
        startFallbackTimer();
        JOptionPane.showMessageDialog(this,
                "Da mat ket noi voi Server (co the Server da tat).\n"
                        + "Dong ho van tiep tuc chay bang gio cua may ban, nhung khong con dieu\n"
                        + "khien duoc Pause/Resume. Bam 'Ket noi lai' de thu ket noi Server khac.",
                "Mat ket noi", JOptionPane.WARNING_MESSAGE);
    }

    /** Khi mất Server: dùng chính đồng hồ hệ thống của máy Client để vẫn hiển thị đúng giờ. */
    private void startFallbackTimer() {
        stopFallbackTimer();
        fallbackTimer = new javax.swing.Timer(1000, e -> updateClockDisplay(LocalTime.now().format(TIME_FMT)));
        fallbackTimer.setInitialDelay(0);
        fallbackTimer.start();
    }

    private void stopFallbackTimer() {
        if (fallbackTimer != null) {
            fallbackTimer.stop();
            fallbackTimer = null;
        }
    }

    private void goBackToConnectScreen() {
        stopFallbackTimer();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        cardLayout.show(cardHolder, "connect");
    }

    private void sendCommand(String command, boolean pausing) {
        if (!connected || out == null) return;
        out.println(command);
        setStatus(true, pausing);
    }

    /** Cập nhật chấm trạng thái + text + bật/tắt đúng nút, dùng chung cho mọi thay đổi trạng thái. */
    private void setStatus(boolean isConnected, boolean isPaused) {
        if (!isConnected) {
            statusDot.setColor(RED_OFFLINE);
        } else if (isPaused) {
            statusDot.setColor(AMBER_PAUSE);
            lblStatusText.setText("Da tam dung");
            btnPause.setEnabled(false);
            btnResume.setEnabled(true);
        } else {
            statusDot.setColor(GREEN_RUN);
            lblStatusText.setText("Dang chay");
            btnPause.setEnabled(true);
            btnResume.setEnabled(false);
        }
        lblStatusIcon.repaint();
        updateDateLabel();
    }

    private void updateDateLabel() {
        LocalDate today = LocalDate.now();
        String weekday = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("vi"));
        lblDate.setText(capitalize(weekday) + ", " + today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TimeClientApp().setVisible(true));
    }
}
