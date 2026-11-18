/*
  MODULE: CHANGELOG — viết bản cập nhật mới vào đây, trang tự cập nhật.

  CÁCH THÊM BẢN MỚI
  Copy nguyên một khối { version: ..., changes: [...] },
  dán LÊN ĐẦU danh sách entries (bản mới nhất nằm trên cùng), rồi sửa nội dung.

  MỖI DÒNG THAY ĐỔI có dạng:   ["loại", "nội dung"],
  Các loại:
    "new"      = Mới
    "fix"      = Sửa lỗi
    "balance"  = Cân bằng
    "change"   = Thay đổi
    "remove"   = Gỡ bỏ
  Bọc chữ trong **hai dấu sao** để in đậm. Có thể thêm  title: "Tên bản cập nhật",  nếu muốn.

  LƯU Ý: giữ nguyên dấu nháy "..." và dấu phẩy , ở cuối mỗi dòng và mỗi khối.
  Nếu gõ thiếu dấu, mục Changelog sẽ tạm ẩn cho đến khi sửa lại (các mục khác vẫn bình thường).
*/
SITE.changelog = {
  title: "Changelog",
  entries: [
    {
      version: "v1.5.2",
      title: "Phát hành trên GitHub",
      date: "18/11/2026",
      changes: [
        ["new", "Đổi tên game thành **Shadow Edict: Ám Lệnh**, biểu tượng và ảnh nền mới."],
        ["new", "Có bản **Windows, macOS, Linux** kèm sẵn Java, giải nén là chơi."],
        ["change", "Máy chủ trực tuyến chuyển sang tên miền mới, kết nối qua HTTPS."],
        ["change", "Hỗ trợ từ **Android 7.0** trở lên."],
        ["fix", "Đại chiến 5v5: không còn bị kẹt khi bám tường hoặc bám mép."],
        ["fix", "Nút Back trên Android: thoát ở menu chính, tạm dừng trong trận."],
        ["fix", "Màn chọn nhân vật không còn bị cắt nút Bắt đầu trên màn thấp."],
      ],
    },
    {
      version: "v1.5",
      title: "Gộp bản của nhóm",
      date: "09/11/2026",
      changes: [
        ["new", "Cốt truyện 3 màn **Thành Cổ, Bến Cảng, Pháo Đài** x 3 ải, có checkpoint và lưu tiến độ."],
        ["new", "Năm loại vũ khí: kiếm, cung, súng, rìu, song đao."],
        ["new", "Phòng 5v5 trực tuyến tới 10 người, chat chữ và đàm thoại micro."],
        ["fix", "Chế độ online kẹt ở \"Đang khởi tạo trận đấu\"."],
        ["fix", "Tỉ số 5v5 khác nhau giữa các máy; mọi người vào cùng một phe."],
        ["fix", "Ải 2-3 không thể qua do sàn sập đặt lệch."],
      ],
    },
    {
      version: "v1.4",
      title: "Đồng bộ giao diện",
      date: "01/11/2026",
      changes: [
        ["change", "Toàn bộ nút, khung, ô nhập thống nhất một kiểu."],
        ["new", "Tiếng Việt có dấu ở mọi màn hình và thông báo trong trận."],
        ["change", "Sắp xếp lại nút cảm ứng theo vùng ngón cái."],
      ],
    },
    {
      version: "v1.2",
      title: "Màn hình chính mới",
      date: "31/10/2026",
      changes: [
        ["new", "Menu Chơi đơn / Chơi mạng, hồ sơ người chơi và hệ thống bạn bè."],
        ["new", "Font tiếng Việt có dấu cho giao diện."],
      ],
    },
    {
      version: "v1.1",
      title: "Rà soát và tối ưu",
      date: "19/10/2026",
      changes: [
        ["fix", "Tốc độ game không còn phụ thuộc FPS."],
        ["fix", "Nhiều lỗi AI và hồi máu trong Đại chiến 5v5."],
        ["fix", "Hợp tác: hai người chơi thấy cùng một bản đồ."],
      ],
    },
  ],
};
