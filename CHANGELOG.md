# CHANGELOG – Assassin's Legend (Heroes of the Void)

## v1.5 – Gộp bản của nhóm + sửa lỗi chế độ trực tuyến (09/11/2026)

Gộp bản sửa mới nhất của nhóm (thư mục `AssasinsLegend 2`, xây trên v1.4) thành **một thư mục duy nhất**.

**Tính năng có trong bản gộp (phần nhóm làm sau v1.4)**
- Cốt truyện viết lại: 3 màn (Thành Cổ, Bến Cảng, Pháo Đài) x 3 ải = 9 ải, bản đồ thiết kế tay với ảnh nền riêng,
  bám mép, bám tường, sàn sập, checkpoint; lưu tiến độ và CHƠI TIẾP (`StorySaveManager`).
- Màn hình **Chọn màn chơi** trên bản đồ cổ (`MapSelectScreen`), **Hồ sơ chiến binh** (`PlayerProfileDialog`).
- Vũ khí: kiếm, cung, súng, rìu, song đao (Tab đổi vũ khí).
- Trực tuyến: phòng 5v5 tới 10 người, chọn phe, chủ phòng chọn bản đồ và bấm BẮT ĐẦU;
  chat chữ + đàm thoại micro (`OnlineCommsManager`, quyền RECORD_AUDIO). 5v5 không còn BOT.

**Lỗi đã sửa trong bản gộp**
| Lỗi | Nguyên nhân | Sửa |
|---|---|---|
| **Chế độ online không vào được trận** (chủ phòng bấm Bắt đầu, kẹt ở "Đang khởi tạo trận đấu...") | Game mới gửi `startGame`, nhưng máy chủ Oracle vẫn chạy `server.js` cũ: máy chủ cũ không biết sự kiện này, chỉ bắt đầu khi CẢ HAI bấm Sẵn sàng | Máy chủ gửi `serverInfo` (giao thức 2) khi kết nối; game nhận ra máy chủ cũ thì báo "Máy chủ bản cũ – cần cập nhật server.js" và tự chuyển sang cách bắt đầu cũ. Ngược lại máy chủ mới vẫn nhận game bản cũ (mọi người Sẵn sàng là tự bắt đầu) |
| Phòng 5v5: mọi người đều vào cùng phe Xanh | Game luôn gửi phe đang chọn (mặc định Xanh) và gửi lại `selectTeam` ngay khi vào phòng | Máy chủ cân bằng phe khi vào phòng; game nhận phe máy chủ xếp, không gửi đè |
| 5v5: tỉ số 30 mạng khác nhau giữa các máy | Chết do tia hồ máu địch / rơi vực chỉ cộng điểm trên máy mình | Phát sự kiện `kill5v5` cho cả phòng |
| 5v5: đứng sát hồ máu địch (54,2–56,5 m) chém người đang hồi máu mà không bị bắn | Vùng tia phòng ngự (3,5 m) hẹp hơn vùng hồi máu (5,8 m) | Hai vùng bằng nhau |
| Sảnh 5v5 vẫn hiện "CHƠI VỚI BOT" nhưng vào trận không có ai | 5v5 đã bỏ BOT | Đổi thành "LUYỆN TẬP MỘT MÌNH", sửa các dòng hướng dẫn |
| **Ải 2-3 không thể qua** | `createCollapsingPlatform` coi tham số là mép trái nhưng mọi lời gọi truyền tâm sàn -> 3 sàn sập lệch phải 1,75 m, vực đầu tiên 5 m (nhảy xa tối đa ≈4,3 m) | Hàm nhận tâm sàn (ải 2-2, 2-3, 3-3 đều dùng) |
| Ải 3-1, 3-2: hố ở cuối bản đồ, vạch đích ải 3-2 nằm trên hố | Sàn cuối dừng ở 122 m / 131 m trong khi bản đồ dài 125 m / 135 m | Sàn cuối kéo tới mép bản đồ (tính theo `mapWidth`) |
| Chơi màn tùy chọn ghi đè tiến độ cốt truyện ("TIẾP TỤC" nhảy sang màn đang chơi thử) | Màn tùy chọn dùng chung đường lưu với cốt truyện | Cờ `freePlayMode`: chơi tự do không ghi checkpoint / tiến độ |
| File lưu của bản 30 ải làm "TIẾP TỤC" trỏ tới ải không còn tồn tại | Không kiểm tra giới hạn | Bỏ qua file lưu ngoài phạm vi 3 màn x 3 ải; kẹp giá trị khi đọc |
| Chọn ngôn ngữ Español/Français/Deutsch/Português/Русский hiện ô trống | Font bitmap không có ñ ç ä ß và chữ Kirin; ngoài bảng Cài đặt game chưa dịch | Chỉ để Tiếng Việt / English (bản dịch khác vẫn giữ trong `LanguageManager`) |
| Thẻ nhân vật ở màn Chọn sát thủ tràn ra 2 mép màn hình; chữ nút "CHỌN MÀN CHƠI (TÙY CHỌN)", "BẮT ĐẦU CHIẾN DỊCH" tràn khỏi nút | Mô tả kỹ năng 1 dòng dài không xuống dòng | Tự xuống dòng, nới nút, rút gọn nhãn |
| Gập/mở máy gập, đổi chế độ tối làm game khởi động lại (mất trận) | `configChanges` trong manifest bị rút gọn | Khôi phục đủ `smallestScreenSize|uiMode|density` và cấu hình Game Mode |
| APK nặng thêm ~10 MB | 4 ảnh trùng lặp đặt tên tiếng Việt có dấu | Xóa bản trùng, giữ `map1_thanhco.png`, `map2_bencang.png`, `map3_phaodai.png`, `chonmanchoi.png` |
| Phiên bản hiển thị 1.3, mục "Có gì mới" còn nói 30 ải / BOT 5v5 | Chưa cập nhật | v1.5 (versionCode 3), viết lại "Có gì mới" |

**Đã kiểm tra**
- Máy chủ: 34/34 trường hợp (thêm: serverInfo, chủ phòng bắt đầu, khách phải sẵn sàng, phòng 5v5 10 người, cân bằng phe 5–5, người thứ 11 bị từ chối).
- Hai bản game chạy thật kết nối máy chủ cục bộ: 1v1, Hợp tác, 5v5 đều vào trận, thấy nhau di chuyển, trừ máu đúng,
  tỉ số 5v5 giống nhau trên 2 máy; với máy chủ v1.4 cũ, cả 3 chế độ vẫn vào trận nhờ chế độ tương thích.
- 9 ải: kiểm tra đường đi tự động (nhảy/thang/dây đu/bệ nhảy theo đúng thông số vật lý) – đi được tới đích cả 9 ải;
  chạy thử từng ải (di chuyển, combo, kỹ năng, tuyệt kỹ, tiệm đồ, đổi vũ khí) không lỗi.

**Việc cần làm khi triển khai**: chép `server/server.js` mới lên máy chủ Oracle rồi khởi động lại (xem `server/README.md`).
Chưa build được APK trong môi trường kiểm thử – cần build lại bằng Android Studio.

## v1.4 – Đồng bộ giao diện & soát lỗi logic (01/11/2026)

**Giao diện thống nhất theo menu chính**
- Toàn bộ nút, khung, ô nhập, thanh cuộn chuyển sang **góc vuông** (bỏ bo tròn): nền tối nhuộm màu vai trò,
  viền 1px, bóng đổ dưới chân; rê chuột/được chọn -> viền sáng + vạch nhấn bên trái; bấm -> lún 2px. Sửa 1 chỗ (`UiKit`) áp dụng cho mọi màn.
- **Font có dấu ở mọi nơi**: `UiKit.small/normal/large/title` và chữ bay trong trận (`damageFont`) dùng font DejaVu có dấu;
  chữ trên nút luôn đậm. Thêm ký tự ✓ ● ◆ ▸ ‹ › ← → vào bộ font.
- **Dịch sang tiếng Việt có dấu**: sảnh 1v1/Hợp tác, sảnh 5v5, Bạn bè, Cài đặt, Lời mời vào trận, màn tải, toàn bộ HUD,
  thông báo trong trận (≈300 câu), tiệm đồ, tạm dừng, bảng thắng/thua, tên bùa, tên BOT, tên mặc định "Chiến Thần".
- **Nút trong trận sắp lại theo vùng ngón cái**: ĐÁNH to nhất sát góc phải; NHẢY bên trái; LƯỚT & TUYỆT KỸ phía trên;
  NGỒI/LEO nhỏ ở ngoài; nút ngữ cảnh (Ưng kích, Ám sát, Bắn pháo, Nói chuyện, Cứu đồng đội) thành 2 cột phía trên.
  Bỏ nút "THOÁT" cạnh nút tạm dừng (dễ bấm nhầm mất trận) – thoát nằm trong menu tạm dừng. Nút chính của bảng thắng màu xanh.
- Sảnh 5v5 dùng ảnh nền không có logo (tiêu đề không bị đè).

**Lỗi logic đã sửa**
| Lỗi | Sửa |
|---|---|
| Mở tiệm đồ khi chơi đơn vẫn bị quái/lính bay đánh | Chơi đơn: mở tiệm = thế giới đứng yên, khóa di chuyển (online vẫn chạy vì không dừng được đối thủ) |
| Vàng trong kho (hiện ở menu) không tiêu được, đầu trận luôn 0 vàng | Chơi đơn: tiệm trừ vàng nhặt trong trận trước, thiếu thì trừ vàng kho; HUD hiện tổng vàng tiêu được ngay từ đầu. Online chỉ dùng vàng trong trận |
| P / B / H vẫn tác dụng khi đang hội thoại (mở tạm dừng/tiệm đè lên hộp thoại) | Khóa tạm dừng, tiệm đồ, uống thuốc trong lúc thoại |
| Thời gian đọc thoại / mua đồ bị tính vào thưởng tốc độ (ải nhiều thoại bị trừ điểm) | Trừ thời gian đứng yên khỏi thưởng tốc độ |
| Nhãn vàng đầu trận không cập nhật tới khi nhặt vàng lần đầu | Cập nhật ngay khi dựng HUD |
| Máy chủ: tìm "chien" không ra "Chiến Thần" | Tìm theo tên không phân biệt dấu & hoa/thường (`server.js`, đã thêm bài test – 26/26 đạt) |

Đã kiểm tra lại: 30 bản đồ vẫn đạt kiểm tra tự động; thoại mở đầu/trùm/kết ải, bảng thắng, thất bại khi Tiểu Vũ hy sinh,
tiệm đồ, tạm dừng, các sảnh, Bạn bè, Cài đặt, trận 5v5 đều chạy trên bản desktop.

## v1.3 – Chế độ cốt truyện "Huyết Lệ Hoàng Triều" (01/11/2026)

Gộp phần cốt truyện của bản `AssasinsLegend (1)` vào bản đang làm, viết lại cho chặt chẽ và tiếng Việt có dấu.
Chi tiết toàn bộ thoại: `COT_TRUYEN_STORY_MODE.md` (sinh tự động từ `StoryChapterConfig.java`).

- **30 ải / 6 chương** (`StoryChapterConfig.java`): mỗi ải có tiêu đề, mục tiêu, thoại mở đầu, thoại gặp trùm, thoại kết ải,
  lời kết; thêm lời dẫn trước ải 1-1 và **2 kết cục** theo nhân vật đã chọn (Vân Tiêu → Ánh Sáng Tân Triều, Vân U → Tro Tàn Vương Quyền).
- **Sửa lỗi logic cốt truyện:** tuổi Tiểu Vũ & thời điểm Trần Quý Phi bị hại; lý do Vân U còn sống (Ám Lệnh) và được cứu ra;
  Hiền Triết = Mặc lão (sư phụ Vân Tiêu), vật trao thống nhất là Hộ Tâm Kính; thuốc giải dùng để cứu Tiểu Vũ ở 4-4;
  trùm cuối thống nhất là Nhiếp Chính Vương; Lão Thái Y / lão thần / Mặc lão có khung chân dung riêng; bỏ người nói "ĐÁNH GIÁ LORE".
- **Hộp thoại điện ảnh:** viền đen trên/dưới, chân dung (p1.jpg/p2.jpg, cậu bé, trùm; nhân vật chưa có ảnh dùng khung chữ cái),
  tên người nói theo màu, chữ chạy, "(nghĩ thầm)", camera zoom về người nói. Space/Enter/E/J/chạm: tiếp (lần đầu hiện hết câu); Esc: bỏ qua cả đoạn.
  Trong lúc thoại: quái, lính bay, Không Tặc, cậu bé đứng yên; phím dùng cho thoại không bị tính thành nhảy/tạm dừng.
- **Tiểu Vũ theo từng ải:** không có (chương 3 – ngục) / giải cứu (1-3) / hộ tống (đi cùng từ đầu). Cậu bé hy sinh ở ải có cậu
  → **NHIỆM VỤ THẤT BẠI**, chơi lại ải. Nút **NÓI CHUYỆN (E)** khi đứng gần: thoại theo chương, lần đầu mỗi ải tặng 1 bình thuốc.
- **Trùm có tên riêng** (Đội Trưởng Tiền Đồn, Thống Chế Phi Đội, Đao Phủ Thiết Giáp, Nữ Thủ Lĩnh Ám Vệ, Đệ Nhất Thần Tướng,
  Nhiếp Chính Vương, Vô Cực Hóa Ma) trên băng cảnh báo, thanh máu và thẻ tên; máu trùm 900 → 2000. Chương 6 bắt đầu với Hộ Tâm Kính (khiên 350).
- **Bản đồ theo chương** (MapTheme): độ rộng vực, độ dài sàn, nhà cao, bục giữa, thang, bụi, dây đu, bệ nhảy và **màu nền/đất**
  riêng cho ngoại thành trăng máu, mái ngói kinh đô, hắc ngục, Ngự Uyển, tường thành, điện Cần Chánh. Ải sau trong chương khó hơn
  một chút; 3-3 Hố Xương Trắng nhiều dây/thang, 5-3 Vọng Lâu nhà cao. Lính bay / Không Tặc chỉ xuất hiện ở ải được cấu hình.
  **Đã kiểm tra tự động cả 30 bản đồ:** không vực nào > 4 m mà thiếu dây đu (vực trống lớn nhất 3,09 m), không quái nào đứng trên vực,
  số quái đúng cấu hình, bản đồ dài dần 90 → 150 m.
- **Tiến độ 30 ải** lưu trong `PlayerProfile` (`maxUnlockedLevel`, `lastStoryLevel`); màn chọn sát thủ có chọn **Chương 1–6 / Ải 1–5**
  (khóa ải chưa mở), mặc định mở sẵn ải đang chơi dở. Thẻ "Cốt truyện" ở menu hiện "Chương X · Ải Y".
- Bảng thắng/thua của chơi đơn tiếng Việt có dấu: lời kết ải, hạ gục/điểm/vàng, nút "TIẾP: CHƯƠNG X · ẢI Y" / "CHƠI LẠI ẢI NÀY".
  Dòng thông báo (actionLog) và tên kẻ địch dùng font có dấu.
- **Không gộp** `StorySaveManager` (lưu máu/vị trí giữa ải) của bản (1): tiến độ đã lưu theo ải trong `PlayerProfile`;
  lưu giữa ải dễ tạo trạng thái lỗi (vào lại ải với 1 HP ngay trước trùm) mà chưa có checkpoint thật trong bản đồ.

## v1.2 – Giao diện màn hình chính mới (31/10/2026)

- **Menu dạng nút bên trái (kiểu Minecraft, đặt như Valorant):** 2 nút lớn **CHƠI ĐƠN** / **CHƠI MẠNG**,
  hàng 2 nút biểu tượng (Bạn bè – chấm đỏ khi có lời mời kết bạn; Có gì mới), hàng **CÀI ĐẶT | THOÁT**.
  **CHƠI MẠNG** mở bảng con: Đại chiến 5v5 / Đấu trường 1v1 / Hợp tác 2 người / Quay lại (Esc).
  Nút được chọn: nền ửng đỏ, viền đỏ, vạch nhấn trái; bấm thì lún xuống. Chọn bằng chuột hoặc phím mũi tên/WASD,
  Enter/Space để mở (mở khi **nhả** phím để màn sau không nhận nhầm cú Enter). Dòng mô tả nút đang chọn hiện ngay dưới.
- **Góc trên trái:** ảnh đại diện chữ cái đầu + tên + `#AL-XXXX`, dòng dưới là chấm & chữ Trực tuyến/Ngoại tuyến.
  Bấm vào đó mở Bạn bè.
- **Bên phải:** thẻ nổi bật Đại chiến 5v5 (ảnh Titan) và thẻ tiến độ (ải đã mở, thanh tiến độ, kỷ lục, vàng).
- **Bỏ phần trùng lặp:** thanh vàng/ải/bánh răng góc trên phải, cột bạn bè mép phải, thẻ "Có gì mới", nút tắt âm nhanh
  (âm thanh chỉnh trong Cài đặt).
- **Đổi tên chuyển sang hộp thoại Bạn bè** (ô tên + nút ĐỔI TÊN cạnh mã ID, gõ được tiếng Việt có dấu);
  Cài đặt chỉ còn âm thanh và phím. Nếu đang mất mạng, tên lưu trên máy và tự gửi lên server khi kết nối lại.
- **Chữ vàng nghiêng nhấp nháy** cạnh logo, câu ngẫu nhiên mỗi lần vào menu; dòng phiên bản ở góc dưới trái.
- **Font tiếng Việt có dấu:** `assets/ui/font_display`, `font_body`, `font_body_bold`, `font_button`, `font_input`
  (.fnt + .png, sinh từ DejaVu Sans Condensed – giấy phép tự do). Dùng qua `UiKit.display()`, `body()`, `bodyBold()`,
  `buttonFont()`, `inputFont()`; thiếu file thì tự quay về font cũ, không crash.
  Các màn khác (Cài đặt, Bạn bè, sảnh, HUD) vẫn dùng font cũ không dấu.

## v1.1 – Rà soát, sửa lỗi & tối ưu (19/10/2026)

Mọi thay đổi đã được biên dịch lại và chạy thử thật trên bản desktop (LWJGL3, OpenGL phần mềm):
menu, chọn nhân vật, SOLO ải 1/2/3/5, thắng ải → qua ải, rơi vực → chơi lại, tạm dừng,
5v5 (75 giây), PVP/CO-OP offline, cửa sổ lệch tỉ lệ, 30/60/120 FPS.
Phần **online** (PVP/CO-OP/bạn bè/lời mời) chưa test được với server thật.

### 1. Lỗi logic/gameplay đã sửa

| # | Lỗi | Nguyên nhân | Cách sửa |
|---|-----|-------------|----------|
| 1 | Tốc độ game phụ thuộc FPS (30 FPS chậm 1/2, 144 Hz nhanh x2.4) | `world.step(1/60f)` gọi **mỗi frame** | Bước vật lý cố định 1/60 s có bộ tích lũy, tối đa 5 bước/frame |
| 2 | 5v5: bot đứng chém không khí mãi | Bot chạy tới bùa nhưng **không có code nhặt bùa**; dừng ở 1.35 m (tầm chém) trong khi tầm nhặt là 1 m | Bot tự nhặt bùa (`applyRune`), khoảng dừng theo loại mục tiêu (địch 1.35 m, bùa/bụi 0.4 m) |
| 3 | 5v5: bot về hồ máu không bao giờ hồi, kẹt ở nhà; người chơi cũng không hồi | `(int)(55 * delta)` = 0 ở 60 FPS | Cộng dồn phần lẻ (accumulator) cho hồi máu & sát thương hồ máu |
| 4 | 5v5: bệ nhảy bắn lên trần, bị bật mãi | Lực 15–16.5 m/s → đỉnh nhảy ~14 m > trần 8.8 m | Tính lại lực theo `v = √(2gh)` (7.0 / 8.0 / 8.6 m/s) |
| 5 | 5v5: bot đẩy người chơi trôi đi, bot chồng lên đầu nhau | Bot có va chạm vật lý với người chơi & nhau | Box2D collision filter: bot chỉ va chạm địa hình |
| 6 | 5v5: bot trên tiền đồn nhắm bùa ngay bên dưới → đứng yên mãi; bot đứng dưới tầng tháp nhảy đập đầu mãi | Không có logic xuống bục; bùa ở tầng trên không với tới | Bot đi ra mép để rơi xuống; chỉ nhắm bùa cùng tầng hoặc thấp hơn |
| 7 | 5v5: Sát thủ bot giật qua lại ở mép bụi | Vào bụi → nhắm địch → ra bụi → nhắm bụi... | Chỉ rời bụi khi địch < 4.5 m |
| 8 | Bùa Phong Tốc (+60% tốc độ) không có tác dụng | `playerSpeedBuff` được gán nhưng không dùng | Áp dụng vào tốc độ chạy & trên không |
| 9 | CO-OP: 2 người chơi ở 2 bản đồ khác nhau | Map random riêng trên từng máy | Seed random theo mã phòng + ải |
| 10 | CO-OP: đánh trúng nhầm quái | Đồng bộ sát thương theo **chỉ số mảng** (thay đổi khi quái chết bị xóa) | Mỗi quái có `id` ổn định |
| 11 | CO-OP: cả đội gục thì kẹt ở màn "Đợi hồi sinh" | Không xử lý trường hợp đồng đội cũng chết/thoát | Hiện màn thua "CẢ ĐỘI ĐÃ GỤC NGÃ" |
| 12 | Quái sinh trên vực rồi rơi chết, người chơi được cộng điểm/vàng "miễn phí" (ải 2: +1500 điểm sau 7 giây đứng yên) | Vị trí sinh không kiểm tra sàn; chết do rơi vực vẫn tính thưởng | Dời vị trí sinh về sàn gần nhất, giới hạn vùng tuần tra, quái dừng ở mép vực, rơi vực không tính thưởng |
| 13 | Ứng Kích (Eagle Strike) gây sát thương **2 lần** lên mục tiêu khóa; lao lệch sang quái khác giữa chừng | Cộng sát thương ở cả vòng lặp và `stopEagleStrike`; mục tiêu đọc lại mỗi frame | Khóa mục tiêu lúc xuất chiêu, áp sát thương đúng 1 lần |
| 14 | Ứng Kích không dùng được trong 5v5 | Chỉ khóa mục tiêu là quái (5v5 không có quái) | Khóa được bot phe địch; vẽ tâm ngắm lên mục tiêu |
| 15 | Hộp thoại thắng ải SOLO hiện nút "ĐỢI HỒI SINH" | TextButton mặc định `visible = true` | Ẩn nút, chỉ hiện ở CO-OP |
| 16 | Phản đòn/đánh chống không khi bị Không Tặc bắt không nạp nộ tuyệt kỹ | Code trùng lặp bỏ sót `onEnemyKilled()` | Dùng chung `damagePirate()` |
| 17 | Dây đu vẽ lệch sang phía đối diện nhân vật | Sai dấu góc xoay (`-angle`) | Dùng `+angle` (đã kiểm chứng bằng ảnh chụp) |
| 18 | Chữ sát thương/tên lệch vị trí khi cửa sổ không đúng 16:9, quá nhỏ trên màn hình lớn | `camera.project()` theo kích thước cửa sổ thật | Chiếu sang hệ UI ảo 1280x720; chữ căn giữa |
| 19 | Tạm dừng: nhân vật P2 bị vẽ thành Sát Thủ Trắng, nền nhảy vị trí | Nhánh pause vẽ riêng | Dùng đúng sprite & nền parallax |
| 20 | Tạm dừng trong PVP/CO-OP làm đóng băng nhân vật trên máy mình (đối thủ vẫn đánh) | Pause dừng toàn bộ cập nhật | Online chỉ mở menu, trận vẫn chạy, khóa điều khiển |

### 2. Lỗi vòng đời / mạng / crash

- `dispose()` được gọi **ngay trong ClickListener/socket callback** (hủy Stage khi nó đang xử lý sự kiện).
  → `Main.setScreen()` tự giải phóng màn hình cũ ở đầu frame kế tiếp; mọi `dispose()` thủ công đã bỏ, có cờ chống dispose 2 lần.
- Listener socket không được gỡ: CO-OP để sót `startGame` → có thể bị kéo vào trận mới **giữa trận**.
  → Gỡ listener ở `hide()` (không phải `dispose()` trễ 1 frame, tránh xóa nhầm listener của màn mới).
- `(String) args[0]` trong `errorMsg` → ClassCastException làm crash game nếu server gửi JSON. → `String.valueOf`.
- Tạo "mã phòng giả" ngẫu nhiên khi chưa có phản hồi server → bỏ; hiển thị trạng thái kết nối thật.
- Arena 5v5 gửi trùng `createRoom` + `createRoom5v5`, 3 sự kiện start → chỉ gửi 1 lần.
- Lời mời bị bỏ qua hiện lại mãi ở mọi màn hình → lời mời hết hạn sau 60 giây, mỗi màn chỉ 1 hộp thoại.
- Rời phòng (Quay lại/ESC/Thoát trận) gửi `leaveRoom` và xóa mã phòng (server chưa xử lý thì socket.io bỏ qua).
- Duyệt danh sách listener trên bản sao (tránh `ConcurrentModificationException`).
- Hộp thoại Bạn bè/Lời mời không chặn click → bấm xuyên được xuống menu phía sau. → Lớp phủ modal chặn click.
- Rò rỉ: font tiêu đề trong MatchInviteDialog, texture overlay, texture joystick, texture dự phòng...
- AndroidManifest: `configChanges` đầy đủ (máy gập, chia màn hình, chế độ tối không khởi động lại game), bật Game Mode API
  (`res/xml/game_mode_config.xml`), bỏ các meta-data không được hệ thống đọc (`isGame`, `app_category`, `game_type`,
  `com.samsung...`, `com.xiaomi...`, `com.miui...`, `com.coloros...`, `com.vivo...`).
- ProGuard/R8 cho bản release: thêm rule giữ `io.socket`, bỏ cảnh báo OkHttp/Okio.

### 3. Hiệu năng

- **VRAM giảm ~6 lần**: sprite 1254x1254 (≈6.3 MB/ảnh, ~330 MB tổng) được thu nhỏ khi nạp xuống 512 px (≈1 MB/ảnh) – `GameAssets`.
- Sprite chuyển filter **Nearest → Linear**: hết răng cưa khi vẽ nhỏ.
- Texture được **cache dùng lại** giữa các trận (chơi lại/qua ải không decode lại ~50 PNG); LoadingScreen nạp trước có thanh tiến trình.
- Bỏ hàm `getBottomPaddingRatio` decode thêm 6 PNG lớn mỗi lần vào trận – kết quả không hề được dùng.
- Bỏ cấp phát mỗi frame: `new TextureRegion` ×2 khi vẽ idle, `new Vector2` trong dây đu/AI bot/chuột, `new Color` trong `getColor()`, nối chuỗi tên 5v5, `String.format` cho nút Lướt/Tuyệt kỹ/choáng.
- HUD chỉ `setText/setStyle` khi giá trị đổi (trước đây `setStyle` mỗi frame làm layout lại toàn bộ HUD).
- Texture tự quản lý (managed) – an toàn khi Android mất OpenGL context.

### 4. Tính năng bổ sung

- **Chọn ải 1–5** ở màn chọn nhân vật; ải được mở khóa & điểm cao nhất được **lưu lại**.
- **Vàng mang về** sau mỗi trận được cộng vào hồ sơ (hiển thị ở menu, bạn bè).
- **Cài đặt**: bật/tắt & âm lượng nhạc nền/hiệu ứng, đổi tên hiển thị, bảng phím tắt – lưu vào Preferences; mở được cả trong trận (menu tạm dừng).
- Thẻ tướng có mô tả & chỉ số, phím tắt (←/→, 1/2, Enter, Esc).
- Trạng thái kết nối server (ONLINE/OFFLINE) ở menu và phòng chờ; tự vào lại phòng khi kết nối lại.
- Tâm ngắm Ứng Kích trên mục tiêu (texture có sẵn nhưng trước đây chưa từng được vẽ).

### 5. Giao diện

- Bộ UI dùng chung `UiKit`: nút bo góc viền vàng, panel tối, font Linear mượt, màu chủ đạo thống nhất.
- Menu chính: bỏ tiêu đề vẽ đè lên logo của ảnh nền, dải tối giúp dễ đọc, hiệu ứng xuất hiện.
- Phòng chờ PVP/CO-OP gộp chung 1 lớp (`LobbyScreen`, trước đây 2 file trùng ~380 dòng), thẻ chọn tướng có ảnh.
- HUD trong trận có nền mờ, thanh máu/minimap vẽ bằng pixel trắng (hết sọc), viền letterbox màu đen.

### 5b. Bạn bè & định danh (bổ sung)

- **Nguyên nhân 2 máy không tìm thấy nhau**: server cũ chưa xử lý bất kỳ sự kiện kết bạn nào.
  Đã viết lại `server/server.js` (giữ nguyên giao thức phòng/trận), kèm bộ test 25 kịch bản đều đạt.
- **Định danh theo máy, không cần tài khoản** (`DeviceIdentity.java`): Android dùng ANDROID_ID
  (giữ nguyên khi gỡ/cài lại app), PC dùng UUID lưu cục bộ. Server cấp mã `AL-XXXX` duy nhất và luôn trả lại
  đúng mã đó cho máy. Không dùng IMEI (Android 10+ cấm app thường đọc IMEI; 4 số cuối dễ trùng).
- Phòng chờ dùng `playerIndex` do server gửi (trước đây chủ phòng quay lại sảnh bị coi là P2).
- Tab lời mời kết bạn hiện tên người gửi.

### 6. File mới

- `GameAssets.java` – bộ đệm texture + thu nhỏ khi nạp.
- `UiKit.java` – font, màu, nút, panel, overlay dùng chung.
- `SettingsDialog.java` – hộp thoại cài đặt.
- `DeviceIdentity.java` – định danh cố định theo máy.
- `server/` – server Socket.IO mới (`server.js`, `package.json`, `README.md`).

### 7. Việc nên làm tiếp (chưa làm vì cần quyết định/công cụ của bạn)

- Xóa file thừa (xem danh sách trong báo cáo).
- Desktop launcher (`lwjgl3/.../Lwjgl3Launcher.java`) đang mở cửa sổ 640x480 (4:3) → đổi `setWindowedMode(1280, 720)`.
- Thu nhỏ ảnh gốc trong `assets/` xuống 512 px để APK nhẹ hơn (~60 MB PNG hiện tại).
- Font FreeType (gdx-freetype) để hiển thị tiếng Việt có dấu.
- Server: xử lý sự kiện `leaveRoom`, nhận vàng từ client nếu muốn đồng bộ vàng online.
- Tách `GameScreen.java` (~7.200 dòng, `render()` ~2.500 dòng) thành các hệ thống nhỏ (Player, Mob AI, 5v5, HUD).
