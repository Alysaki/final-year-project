# HƯỚNG DẪN ĐẤU NỐI CELLULAR & KIỂM THỬ CLOUD (MÃ DỰ ÁN: 1682)

Tài liệu này hướng dẫn chi tiết cách thiết lập phần cứng cho module di động SIM900A và định vị vệ tinh GPS GT-U7 trên ESP32, cách làm mạch phân áp bảo vệ logic, cắm tụ lọc sụt áp, và các câu lệnh PlatformIO CLI nạp mạch cho các Giai đoạn 4 (`s4`), 6 (`s6`), và 7 (`s7`).

---

## 1. HƯỚNG DẪN ĐẤU NỐI PHẦN CỨNG & BẢO VỆ MẠCH

### 1.1 Mạch phân áp giảm áp từ TX (SIM900A - 5V) xuống GPIO 34 (ESP32 - 3.3V)
> [!WARNING]
> Chân TX của SIM900A xuất mức logic **5V**, trong khi các chân GPIO của ESP32 (đặc biệt là GPIO 34 - chân **Input-Only**) chỉ chịu được mức điện áp tối đa **3.3V**. Đấu nối trực tiếp 5V vào ESP32 sẽ gây hỏng chip vĩnh viễn.

**Sơ đồ mạch phân áp dùng 2 điện trở:**
Ta cần tạo ra tỉ số phân áp khoảng $\frac{2}{3}$. Sử dụng 1 điện trở $390\Omega$ nối tiếp và 2 điện trở $390\Omega$ song song xuống đất (tương đương $195\Omega$) như thiết kế tại mục 1.7 của `CONTEXT_ROBOT.md`:

```
TX_SIM (5V) ───[ 390 Ω ]───┬─── P34 (ESP32 Input-Only, ~3.33V)
                           │
                        [ 195 Ω ] (Gồm 2 điện trở 390 Ω đấu song song)
                           │
                          GND (Nối đất chung)
```

**Cách kết nối dây thực tế:**
1. Lấy chân **TXD** trên SIM900A nối vào một đầu điện trở $390\Omega$.
2. Đầu còn lại của điện trở $390\Omega$ này nối chung với:
   - Chân **GPIO 34** (Cọc P34) trên Shield SVG của ESP32.
   - Đầu vào của cụm trở $195\Omega$ xuống GND.
3. Đầu còn lại của cụm trở $195\Omega$ nối trực tiếp vào chân **GND** (Cọc G) trên Shield.
4. Chân **RXD** trên SIM900A nối trực tiếp vào chân **GPIO 25** (Cọc P25) của ESP32 (vì ESP32 xuất mức logic 3.3V, SIM900A nhận diện an toàn không cần phân áp).

---

### 1.2 Vị trí cắm tụ lọc nguồn 680uF cho SIM900A
> [!IMPORTANT]
> Module SIM900A khi truyền phát sóng (burst transmission) tiêu thụ dòng điện rất lớn (có thể lên tới **2A** trong thời gian ngắn). Nếu nguồn cấp không đủ khỏe hoặc bị sụt áp, module sẽ tự động tắt hoặc liên tục tự reset.

Ta sử dụng một tụ hóa dung lượng **680uF** (hoặc lớn hơn, ví dụ 1000uF, điện áp chịu đựng tối thiểu 10V) mắc song song tại nhánh nguồn cấp cho SIM900A ngay sau mạch Buck LM2596 #1:

```
LM2596 Buck #1 (5V OUT) ────┬───────────────────────── VCC (SIM900A)
                            │   + (Chân dài / Không có vạch)
                        [ Tụ Hóa ] 680uF / 16V
                            │   - (Chân ngắn / Có vạch trắng "-")
GND ────────────────────────┴───────────────────────── GND (SIM900A)
```
- **Lưu ý cực tính**: Tụ hóa có phân biệt cực âm và cực dương. Cắm ngược cực sẽ gây nổ tụ. Chân có vạch sọc màu trắng kèm ký hiệu trừ `-` là cực âm, đấu vào **GND**. Chân còn lại là cực dương, đấu vào đường **5V**.

---

### 1.3 Bảng đấu nối tổng thể Cellular & GPS

| Thiết bị | Chân thiết bị | Chân trên Shield SVG | Ghi chú |
| :--- | :--- | :--- | :--- |
| **GPS GT-U7** | **VCC** | **5V** (hoặc 3.3V) | Cấp nguồn cho GPS |
| | **GND** | **GND** | Nối đất chung |
| | **TXD** | **GPIO 16** (RX2) | Nhận bản tin NMEA từ GPS |
| | **RXD** | **GPIO 17** (TX2) | Gửi lệnh cấu hình GPS (nếu cần) |
| **SIM900A** | **VCC / 5V** | **5V OUT** của Buck #1 | Nguồn cấp độc lập dòng cao |
| | **GND** | **GND** | Nối đất chung |
| | **TXD** | **GPIO 34** (RX_ESP) | Truyền lệnh AT (Qua mạch phân áp) |
| | **RXD** | **GPIO 25** (TX_ESP) | Nhận lệnh AT trực tiếp |

---

## 2. HƯỚNG DẪN BIÊN DỊCH & NẠP FIRMWARE (PLATFORMIO CLI)

Để biên dịch và nạp riêng từng tệp kiểm thử, hãy chạy các câu lệnh dưới đây trong Terminal của PlatformIO:

### 2.1 Kiểm thử Giai đoạn 4: GPS GT-U7 (`stage4_gps.cpp`)
Chương trình đọc dữ liệu GPS qua HardwareSerial2 và parse hiển thị Lat/Lng:
```powershell
# Biên dịch và nạp firmware
pio run -e s4 -t upload

# Mở cổng giám sát Serial
pio device monitor
```

### 2.2 Kiểm thử Giai đoạn 6: Điều khiển động cơ qua GPRS (`stage6_gprs_control.cpp`)
Chương trình kết nối GPRS mạng Mobifone và định kỳ 2 giây kéo lệnh từ Mock Server để điều khiển bánh xe:
```powershell
# Biên dịch và nạp firmware
pio run -e s6 -t upload

# Mở cổng giám sát Serial
pio device monitor
```

### 2.3 Kiểm thử Giai đoạn 7: Đồng bộ dữ liệu lên Firebase (`stage7_firebase_sync.cpp`)
Chương trình đồng bộ tọa độ định vị GPS lên đám mây Firebase Realtime Database qua mạng di động GPRS bọc SSL bảo mật:
```powershell
# Biên dịch và nạp firmware
pio run -e s7 -t upload

# Mở cổng giám sát Serial
pio device monitor
```
> [!NOTE]
> - Đối với **Giai đoạn 7**, hãy mở tệp `src/stages/stage7_firebase_sync.cpp` và thay thế các chuỗi cấu hình `API_KEY` và `DATABASE_URL` thực tế của bạn trước khi chạy lệnh nạp mạch.
