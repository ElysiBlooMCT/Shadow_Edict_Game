/*
  MODULE: HÌNH ẢNH TRONG GAME
  Mỗi ảnh là một khối { thumb, full, title, text }.
  - thumb: ảnh nhỏ hiện trong lưới (nên rộng 640px), full: ảnh lớn khi bấm vào xem (1280px).
  - Ảnh đặt trong thư mục assets/img/shots/.
  - more: nút "Xem tất cả" hiện ở trang chủ (trang chủ chỉ hiện vài ảnh đầu, số ảnh đặt bằng data-limit trong index.html).
  Thứ tự các khối là thứ tự ảnh trên trang.
*/
SITE.gallery = {
  title: "Hình ảnh trong game",
  more: { label: "Xem tất cả ảnh", url: "/gallery/", style: "secondary" },
  items: [
    {
      thumb: "img/shots/menu-s.jpg",
      full: "img/shots/menu.jpg",
      title: "Màn hình chính",
      text: "Cốt truyện, chọn màn, chơi mạng, bạn bè và cài đặt. Thẻ bên phải hiện chế độ nổi bật và tiến độ cốt truyện.",
    },
    {
      thumb: "img/shots/thanh-co-s.jpg",
      full: "img/shots/thanh-co.jpg",
      title: "Màn 1 · Thành Cổ",
      text: "Khởi binh ngoại thành: làm quen nhịp chạy nhảy, vượt thùng hàng và bám mép leo lên ban công.",
    },
    {
      thumb: "img/shots/ben-cang-s.jpg",
      full: "img/shots/ben-cang.jpg",
      title: "Màn 2 · Bến Cảng",
      text: "Nhảy giữa các đoạn cầu cảng gãy trên mặt nước sâu, leo thùng hàng vào sân kho cảng.",
    },
    {
      thumb: "img/shots/phao-dai-s.jpg",
      full: "img/shots/phao-dai.jpg",
      title: "Màn 3 · Pháo Đài",
      text: "Dốc đá nguy hiểm: chinh phục bậc đá dốc đứng, bám mép tường thành đổ nát.",
    },
    {
      thumb: "img/shots/thoai-cot-truyen-s.jpg",
      full: "img/shots/thoai-cot-truyen.jpg",
      title: "Hội thoại cốt truyện",
      text: "Mỗi ải mở đầu bằng đoạn thoại có chân dung nhân vật. Bấm Space / chạm để đọc tiếp, Esc để bỏ qua.",
    },
    {
      thumb: "img/shots/dai-chien-5v5-s.jpg",
      full: "img/shots/dai-chien-5v5.jpg",
      title: "Đại chiến 5v5",
      text: "Tung tuyệt kỹ giữa trận đại chiến. Phe nào đủ 30 mạng hạ gục trước thì thắng.",
    },
    {
      thumb: "img/shots/5v5-thap-s.jpg",
      full: "img/shots/5v5-thap.jpg",
      title: "5v5 · Leo tháp tiền đồn",
      text: "Thang, bục nhảy và bùa tranh chấp nằm rải rác trên bản đồ.",
    },
    {
      thumb: "img/shots/chon-sat-thu-s.jpg",
      full: "img/shots/chon-sat-thu.jpg",
      title: "Chọn sát thủ",
      text: "**Vân Tiêu** – Bạch Y Kiếm Thánh hoặc **Vân U** – Hắc Dạ Sát Thủ, mỗi người một bộ kỹ năng và tuyệt kỹ riêng.",
    },
    {
      thumb: "img/shots/chon-man-s.jpg",
      full: "img/shots/chon-man.jpg",
      title: "Chọn màn chơi",
      text: "Bản đồ cổ với 3 màn: Thành Cổ Thời Chiến, Bến Cảng Ban Đêm, Pháo Đài Trên Núi.",
    },
    {
      thumb: "img/shots/tiem-do-s.jpg",
      full: "img/shots/tiem-do.jpg",
      title: "Tiệm đồ sát thủ",
      text: "Dùng vàng nâng cấp vũ khí, giáp và mua bình máu ngay trong ải (phím B).",
    },
    {
      thumb: "img/shots/sanh-5v5-s.jpg",
      full: "img/shots/sanh-5v5.jpg",
      title: "Sảnh Đại chiến 5v5",
      text: "Chọn phe Ánh Sáng hoặc Bóng Tối, tạo phòng và mời bạn bè, hoặc luyện tập một mình.",
    },
    {
      thumb: "img/shots/dau-truong-1v1-s.jpg",
      full: "img/shots/dau-truong-1v1.jpg",
      title: "Đấu trường 1v1",
      text: "Tạo phòng hoặc nhập mã phòng, chọn sát thủ rồi bấm Sẵn sàng.",
    },
  ],
};
