package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Xử lý 1 Client cụ thể. STATEFUL nghĩa là: mỗi ClientSession tự giữ biến "paused"
 * CỦA RIÊNG MÌNH — Client A bấm Pause không ảnh hưởng gì tới Client B đang chạy.
 *
 * TỐI ƯU XỬ LÝ (so với bản trước): CHỈ CẦN 1 Thread cho mỗi Client (CommandListener,
 * chuyên nghe lệnh PAUSE/RESUME) — KHÔNG còn Thread "Sender" riêng cho từng người.
 * Việc gửi giờ được chuyển thành hàm tick(), do TimeServer gọi từ 1 bộ đếm giờ DÙNG
 * CHUNG cho mọi Client (xem TimeServer.java). Lợi ích:
 *  - Giảm ~50% số Thread khi có nhiều Client (N client: trước 2N Thread, giờ chỉ N+1).
 *  - Mọi Client nhận giờ ở ĐÚNG CÙNG 1 THỜI ĐIỂM (1 nguồn đồng hồ duy nhất), thay vì
 *    N Thread độc lập, mỗi Thread lệch nhau vài mili-giây do lịch trình hệ điều hành.
 */
public class ClientSession {

    private final Socket socket;
    private final String clientLabel; // để log cho dễ phân biệt client nào
    private final Consumer<ClientSession> onDisconnect; // callback báo Server gỡ mình khỏi danh sách

    // AtomicBoolean vì có 2 "phía" cùng đọc/ghi: Thread CommandListener (ghi) và
    // Thread lập lịch dùng chung của Server (đọc, trong tick())
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private PrintWriter out;
    private BufferedReader in;

    public ClientSession(Socket socket, Consumer<ClientSession> onDisconnect) {
        this.socket = socket;
        this.clientLabel = socket.getRemoteSocketAddress().toString();
        this.onDisconnect = onDisconnect;
    }

    /** Gọi 1 lần khi Client vừa kết nối: mở luồng đọc/ghi, khởi động Thread nghe lệnh. */
    public void start() throws IOException {
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        System.out.println("[+] Client ket noi: " + clientLabel);

        Thread commandListener = new Thread(this::listenForCommands, "cmd-" + clientLabel);
        commandListener.setDaemon(true);
        commandListener.start();
    }

    /**
     * Được GỌI TỪ BÊN NGOÀI bởi bộ đếm giờ dùng chung của TimeServer, đúng mỗi giây 1 lần,
     * áp dụng cho TẤT CẢ Client đang kết nối. ClientSession không tự đếm giờ nữa.
     */
    public void tick(String timeStr) {
        if (paused.get()) return; // đang pause -> bỏ qua lượt này, không gửi
        out.println(timeStr);
        if (out.checkError()) {
            // checkError() = true nghĩa là ghi thất bại -> Client đã ngắt kết nối
            disconnect();
        }
    }

    /** Chạy trên Thread riêng (DUY NHẤT của Client này), đọc liên tục các dòng lệnh gửi lên. */
    private void listenForCommands() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String command = line.trim().toUpperCase();
                if (command.equals("PAUSE")) {
                    paused.set(true);
                    System.out.println("[PAUSE] " + clientLabel);
                } else if (command.equals("RESUME")) {
                    paused.set(false);
                    System.out.println("[RESUME] " + clientLabel);
                }
                // Lệnh lạ khác -> bỏ qua, không làm crash server
            }
        } catch (IOException e) {
            // Socket đóng (Client tắt đột ngột) -> vòng lặp tự dừng
        } finally {
            disconnect();
        }
    }

    private void disconnect() {
        try { socket.close(); } catch (IOException ignored) {}
        System.out.println("[-] Client ngat ket noi: " + clientLabel);
        onDisconnect.accept(this); // báo Server gỡ mình khỏi danh sách đang tick
    }
}
