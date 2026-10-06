# THỰC HÀNH 3 — TCP TimeServer/TimeClient (Server song song, stateful)

Đã biên dịch và **kiểm thử tự động thành công** (2 Client giả lập, không qua GUI) — xem mục
"Đã kiểm thử" cuối file.

## 1. Kiến trúc

```
server/
  TimeServer.java     main — ServerSocket, bộ đếm giờ DÙNG CHUNG (ScheduledExecutorService),
                       in ra IP LAN, accept loop
  ClientSession.java  xử lý 1 Client: CHỈ 1 Thread (nghe lệnh Pause/Resume), việc gửi giờ
                       chuyển thành hàm tick() do bộ đếm chung của Server gọi mỗi giây.
                       Tự giữ biến "paused" RIÊNG cho từng Client (đúng nghĩa "stateful")
client/
  TimeClientApp.java  GUI chính (màn hình kết nối + màn hình đồng hồ)
  AppFonts.java        nạp 2 font riêng (Share Tech Mono cho đồng hồ, Space Grotesk cho chữ)
  RoundedButton.java   nút bấm tự vẽ, bo góc, đổi màu theo trạng thái
  RoundedPanel.java    panel nền bo góc ("thẻ" chứa nội dung)
  StatusDot.java       chấm tròn báo trạng thái (xanh lá = đang chạy, vàng = tạm dừng, đỏ = mất kết nối)
```

## 2. Vì sao mỗi Client vẫn cần ít nhất 1 Thread riêng ở phía Server?

Server phải làm 2 việc CÙNG LÚC cho 1 Client:
1. Cứ mỗi giây gửi 1 dòng giờ xuống.
2. Bất cứ lúc nào cũng phải sẵn sàng nhận lệnh PAUSE/RESUME Client gửi lên.

Việc (1) giờ được **1 bộ đếm giờ DÙNG CHUNG** của `TimeServer` đảm nhiệm cho MỌI Client cùng
lúc (xem mục 6.5), không cần Thread riêng cho từng người nữa. Nhưng việc (2) — lắng nghe lệnh —
là thao tác **chặn (blocking)**: `BufferedReader.readLine()` sẽ đứng chờ vô thời hạn tới khi
Client gửi gì đó, nên mỗi Client vẫn cần đúng 1 Thread riêng dành cho việc này (không thể gộp
nhiều Client vào 1 Thread nghe lệnh chung, vì Thread đó sẽ bị "kẹt" chờ đúng 1 người mà bỏ lỡ
người khác).

**"Stateful" nghĩa là gì trong bài này?** Mỗi `ClientSession` là 1 object riêng cho từng Client,
biến `paused` nằm BÊN TRONG object đó — Client A và Client B có 2 biến `paused` hoàn toàn tách
biệt nhau. Server không dùng 1 biến `paused` DÙNG CHUNG cho tất cả Client (nếu dùng chung, A
bấm Pause sẽ làm B cũng bị dừng theo — SAI với yêu cầu đề bài). Bộ đếm giờ dùng chung ở mục 6.5
chỉ dùng chung phần "đếm giờ", KHÔNG dùng chung phần "paused" — 2 khái niệm tách biệt nhau.

## 3. Giao thức (đơn giản, dùng text thuần qua `PrintWriter`/`BufferedReader`)

- Server → Client: mỗi dòng là 1 chuỗi giờ dạng `HH:mm:ss`, gửi mỗi giây (trừ lúc đang pause).
- Client → Server: gửi đúng 1 dòng `PAUSE` hoặc `RESUME` khi bấm nút tương ứng.

## 4. Cách chạy TRÊN CÙNG 1 MÁY (test nhanh trước)

```bash
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java")
cp -r src/main/resources/fonts out/fonts   # QUAN TRỌNG: copy font vào cùng thư mục out

# Cửa sổ 1: Server
java -cp out server.TimeServer

# Cửa sổ 2, 3...: Client (mở nhiều lần để test song song)
java -cp out client.TimeClientApp
```
Ở màn hình kết nối của Client, gõ IP `127.0.0.1`, Port `5959` (mặc định điền sẵn), bấm Kết nối.

## 5. Cách chạy TRÊN 2 MÁY KHÁC NHAU trong cùng mạng LAN (đúng yêu cầu đề bài)

1. **Máy chạy Server**: chạy `server.TimeServer` như trên. Console sẽ tự in ra danh sách địa
   chỉ IP LAN của máy, ví dụ:
   ```
   Cac dia chi IP LAN cua may nay (Client o may khac dung 1 trong so nay):
      -> 192.168.1.15   (Wi-Fi)
   ```
   **Ghi nhớ đúng địa chỉ này** (không phải `127.0.0.1` — đó chỉ dùng được khi Client cùng máy).
2. **Firewall**: lần đầu chạy Server, Windows Defender Firewall có thể hiện hộp thoại hỏi
   "Allow Java to communicate on these networks" — bấm **Allow access** (tick cả Private và
   Public network nếu cần). Nếu không thấy hộp thoại này mà Client vẫn không kết nối được, vào
   Windows Defender Firewall → Cho phép 1 ứng dụng → thêm `java.exe`, tick cả 2 cột Private/Public.
3. **Máy chạy Client**: đảm bảo **cùng mạng Wi-Fi/LAN** với máy Server. Chạy `client.TimeClientApp`,
   ở màn hình kết nối gõ đúng địa chỉ IP đã ghi ở bước 1 (ví dụ `192.168.1.15`), Port `5959`,
   bấm Kết nối.
4. Có thể mở nhiều Client trên nhiều máy khác nhau cùng lúc, mỗi máy Pause/Resume độc lập.

## 6. Đã kiểm thử

Biên dịch bằng JDK 21, chạy `TestHarness` tự động (2 Client giả lập bằng Socket thuần, không
qua GUI) — toàn bộ PASS:
```
A và B cùng nhận giờ mỗi giây, độc lập nhau (server song song)
A gửi PAUSE  -> A KHÔNG nhận thêm giờ nào nữa
               B VẪN tiếp tục nhận giờ bình thường (server stateful, không lẫn trạng thái)
A gửi RESUME -> A nhận lại giờ như cũ
```
Phần GUI (`TimeClientApp`) không tự test được (cần màn hình), nhưng toàn bộ logic mạng mà GUI
gọi xuống (Socket, PrintWriter, BufferedReader) dùng đúng giao thức đã được kiểm chứng ở trên.

## 6.5. Tối ưu kết nối LAN, đồng bộ giờ, băng thông/lưu trữ/xử lý (cập nhật gần đây)

**Kết nối LAN:**
- **`setReuseAddress(true)`** trên `ServerSocket` — trước đây nếu tắt Server rồi bật lại quá
  nhanh (lúc đang sửa code, test liên tục) sẽ gặp lỗi `Address already in use` do hệ điều hành
  chưa kịp giải phóng cổng. Đã kiểm chứng: kill Server rồi bật lại ngay (cách nhau 0.3 giây)
  không còn lỗi.
- **Client kết nối có timeout rõ ràng (5 giây)** thay vì có thể treo vô thời hạn khi Firewall
  âm thầm chặn gói tin — và có 3 thông báo lỗi khác nhau tùy nguyên nhân cụ thể (hết thời gian
  chờ / Server từ chối / không tìm thấy địa chỉ IP) thay vì 1 thông báo lỗi chung chung.

**Đồng bộ giờ (gần như lệch = 0):**
- Trước đây mỗi `ClientSession` tự `Thread.sleep(1000)` độc lập, dễ lệch dần theo thời gian
  chạy VÀ lệch nhau giữa các Client (mỗi Thread được hệ điều hành lên lịch không hoàn toàn
  đồng thời). Đã sửa bằng cách chuyển toàn bộ việc đếm giờ về **1 bộ đếm giờ DÙNG CHUNG duy
  nhất** ở `TimeServer` (`ScheduledExecutorService.scheduleAtFixedRate`, căn đúng mốc giây thực
  làm initialDelay) — mọi Client nhận giờ từ ĐÚNG CÙNG 1 nguồn, tại ĐÚNG CÙNG 1 thời điểm.
  Đã kiểm chứng 2 điều bằng test tự động: (1) khoảng cách giữa các lần nhận giờ ổn định ở mức
  999-1001ms; (2) 2 Client A và B nhận được **đúng cùng 1 giá trị giờ** ở cùng 1 thời điểm.

**Băng thông:** mỗi giây chỉ gửi 1 chuỗi ngắn `"HH:mm:ss"` (~8 byte) cho mỗi Client đang hoạt
động, và **không gửi gì cả khi Client đang Pause** (không lãng phí băng thông cho người không
cần cập nhật). Bật thêm `TCP_NODELAY` ở cả 2 phía để mỗi gói tin được gửi đi ngay khi gọi,
không bị hệ điều hành giữ lại chờ gộp chung với gói khác — đánh đổi 1 chút overhead giao thức
TCP/IP (không đáng kể ở quy mô 1 tin nhắn/giây) để lấy độ trễ hiển thị thấp nhất.

**Lưu trữ:** Server và Client đều KHÔNG ghi bất kỳ dữ liệu nào xuống đĩa (không log ra file,
không dùng CSDL) — mọi trạng thái (biến `paused`, danh sách Client đang online) chỉ tồn tại
tạm thời trong RAM, tự giải phóng khi Client ngắt kết nối. Không có gì để tối ưu thêm ở khía
cạnh này vì dung lượng sử dụng gần như bằng 0.

**Xử lý (CPU/Thread) — cải tiến chính:** trước đây mỗi Client tốn **2 Thread riêng** (1 Thread
tự đếm giờ gửi xuống + 1 Thread nghe lệnh) — với N Client sẽ tốn **2N Thread**. Đã tái cấu trúc
thành **1 bộ đếm giờ dùng chung cho toàn Server** (chỉ 1 Thread) + mỗi Client chỉ còn giữ đúng
1 Thread (nghe lệnh Pause/Resume, việc này bắt buộc phải blocking riêng từng người) — N Client
giờ chỉ tốn **N+1 Thread**, giảm gần một nửa. Với 1 lớp học ~30 máy Client kết nối cùng lúc:
trước đây 60 Thread, giờ chỉ 31 Thread — tiết kiệm đáng kể bộ nhớ ngăn xếp (mỗi Thread Java mặc
định chiếm ~512KB-1MB) và giảm chi phí chuyển đổi ngữ cảnh (context switching) của hệ điều hành.
Đã kiểm chứng lại toàn bộ tính năng (song song/stateful/pause/resume/ngắt kết nối đột ngột) sau
khi tái cấu trúc — tất cả vẫn PASS đúng như trước.

- **Client kết nối có timeout rõ ràng (5 giây)** thay vì có thể treo vô thời hạn khi Firewall
  âm thầm chặn gói tin — và có 3 thông báo lỗi khác nhau tùy nguyên nhân cụ thể (hết thời gian
  chờ / Server từ chối / không tìm thấy địa chỉ IP) thay vì 1 thông báo lỗi chung chung.

## 7. Giao diện

Giao diện tối màu (dark flat design), dùng 2 font riêng đóng gói sẵn trong `resources/fonts`
(không phụ thuộc font có sẵn trên máy):
- **Share Tech Mono** — font kiểu đồng hồ điện tử, dùng cho ô hiển thị giờ lớn.
- **Space Grotesk** — font sans hiện đại, dùng cho tiêu đề/nút bấm/nhãn.

Chấm tròn trạng thái đổi màu: xanh lá (đang chạy) / vàng (đã tạm dừng) / đỏ (mất kết nối) —
tự vẽ bằng `Graphics2D`, không dùng ảnh icon có sẵn.

**Đồng hồ có 2 kiểu hiển thị** (chuyển qua lại bằng nút "SO"/"KIM"): số điện tử và kim đồng hồ
tự vẽ (mặt số + kim giờ/phút/giây, có vạch chia phút).

**Khi mất kết nối Server**: đồng hồ KHÔNG đứng khựng lại, mà tự chuyển sang chạy bằng giờ hệ
thống của chính máy Client (vẫn đúng giờ thực), có nhãn "Ngoại tuyến" và nút "KẾT NỐI LẠI" để
quay về màn hình đăng nhập. Pause/Resume bị khóa vì không còn Server để gửi lệnh tới.

**LƯU Ý KHI MỞ TRONG INTELLIJ:** vì font nằm trong `src/main/resources`, cần đánh dấu thư mục
`resources` là **Resources Root** (chuột phải > Mark Directory as > Resources Root) để IntelliJ
tự copy file font vào classpath khi Run — nếu không làm bước này, chương trình vẫn chạy được
nhưng sẽ dùng font mặc định của hệ điều hành thay vì font đã thiết kế.

