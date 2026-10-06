package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server song song (mỗi Client 1 ClientSession), stateful (mỗi ClientSession tự giữ
 * trạng thái pause/resume của riêng nó).
 *
 * TỐI ƯU XỬ LÝ: dùng 1 ScheduledExecutorService DÙNG CHUNG cho mọi Client, thay vì để
 * mỗi Client tự chạy 1 Thread riêng "ngủ 1 giây rồi gửi". Lợi ích:
 *  - Ít Thread hơn hẳn: N Client trước đây tốn 2N Thread (Sender + CommandListener mỗi
 *    người), giờ chỉ còn N+1 (1 CommandListener/Client + 1 bộ đếm giờ dùng chung).
 *  - Đồng bộ TUYỆT ĐỐI giữa các Client: tất cả nhận giờ ở ĐÚNG CÙNG 1 THỜI ĐIỂM (cùng 1
 *    nguồn tick), thay vì N Thread độc lập có thể lệch nhau vài mili-giây.
 *  - Dùng đúng công cụ JDK built-in cho việc "chạy định kỳ" thay vì tự viết lại logic
 *    Thread.sleep() thủ công.
 *
 * new ServerSocket() + setReuseAddress(true): cho phép khởi động lại ngay sau khi vừa
 * tắt, không bị lỗi "Address already in use" do hệ điều hành giữ cổng ở trạng thái chờ.
 */
public class TimeServer {

    public static final int PORT = 5959;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Danh sách MỌI Client đang online, dùng chung cho bộ đếm giờ -> phải thread-safe
    // (Thread accept() ghi thêm, Thread lập lịch đọc để tick, Thread disconnect() xoá bớt)
    private static final Set<ClientSession> sessions = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        System.out.println("=== TCP Time Server ===");
        System.out.println("Dang lang nghe tai cong " + PORT + " ...");
        printLanAddresses();

        startSharedClock();

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true); // gửi ngay, không gộp gói tin -> giảm độ trễ

                ClientSession session = new ClientSession(clientSocket, sessions::remove);
                try {
                    session.start();
                    sessions.add(session); // chỉ thêm vào danh sách "sẽ được tick" sau khi start() thành công
                } catch (IOException e) {
                    System.err.println("Loi khoi tao client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Loi server: " + e.getMessage());
        }
    }

    /**
     * Bộ đếm giờ DÙNG CHUNG duy nhất cho toàn Server — chỉ 1 Thread nền, đúng mỗi giây
     * gọi tick() cho MỌI Client đang có trong danh sách "sessions".
     */
    private static void startSharedClock() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "shared-clock");
            t.setDaemon(true);
            return t;
        });

        // Căn initialDelay đúng tới mốc giây thực tiếp theo, để tick ĐẦU TIÊN cũng khớp
        // ranh giới giây thật (không phải cứ chạy xong dòng lệnh này là tick ngay lập tức).
        long msIntoCurrentSecond = System.currentTimeMillis() % 1000;
        long initialDelay = 1000 - msIntoCurrentSecond;

        // scheduleAtFixedRate: JDK tự đảm bảo mỗi lần chạy cách nhau ĐÚNG 1000ms theo lịch
        // gốc, không cộng dồn sai số dù mỗi lần chạy tốn chút thời gian xử lý — đúng công
        // cụ chuẩn cho bài toán "chạy định kỳ", không cần tự viết lại bằng Thread.sleep().
        scheduler.scheduleAtFixedRate(() -> {
            String now = LocalTime.now().format(FORMAT);
            for (ClientSession session : sessions) {
                session.tick(now);
            }
        }, initialDelay, 1000, TimeUnit.MILLISECONDS);
    }

    /** In ra tất cả địa chỉ IP LAN của máy này, để biết Client ở máy khác phải gõ IP nào. */
    private static void printLanAddresses() {
        System.out.println("Cac dia chi IP LAN cua may nay (Client o may khac dung 1 trong so nay):");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                for (InetAddress addr : Collections.list(addresses)) {
                    if (addr.getHostAddress().indexOf(':') == -1) { // bỏ qua IPv6 cho gọn
                        System.out.println("   -> " + addr.getHostAddress() + "   (" + ni.getDisplayName() + ")");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   (Khong doc duoc danh sach IP: " + e.getMessage() + ")");
        }
        System.out.println("Neu Client chay CUNG may nay, dung 127.0.0.1 hoac localhost.");
    }
}
