/*
  MODULE: TRANG CHỦ — phần "Khám phá" bên dưới thẻ phiên bản
  Mỗi thẻ dẫn tới một trang. Link nội bộ viết dạng "/tên-trang/".
  Muốn bỏ một thẻ thì xoá cả khối { ... } của nó.
*/
SITE.home = {
  title: "Khám phá Ám Lệnh",
  columns: 3,
  items: [
    {
      icon: "◆",
      title: "Giới thiệu",
      text: "Câu chuyện, nhân vật và những gì có trong game.",
      buttons: [{ label: "Xem chi tiết", url: "/about/", style: "secondary" }],
    },
    {
      icon: "✦",
      title: "Chế độ chơi",
      text: "Cốt truyện, Đại chiến 5v5, đấu 1v1 và Hợp tác 2 người.",
      buttons: [{ label: "Xem chi tiết", url: "/modes/", style: "secondary" }],
    },
    {
      icon: "✧",
      title: "Điều khiển",
      text: "Phím trên máy tính và nút cảm ứng trên điện thoại.",
      buttons: [{ label: "Xem chi tiết", url: "/controls/", style: "secondary" }],
    },
    {
      icon: "◆",
      title: "Tải game",
      text: "Android, Windows, macOS và Linux. Miễn phí.",
      buttons: [{ label: "Tải ngay", url: "/download/", style: "primary" }],
    },
    {
      icon: "✦",
      title: "Changelog",
      text: "Theo dõi các bản cập nhật mới nhất.",
      buttons: [{ label: "Xem chi tiết", url: "/changelog/", style: "secondary" }],
    },
    {
      icon: "✧",
      title: "FAQ",
      text: "Giải đáp nhanh các câu hỏi thường gặp.",
      buttons: [{ label: "Xem chi tiết", url: "/faq/", style: "secondary" }],
    },
  ],
};
