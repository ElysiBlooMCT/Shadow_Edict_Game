/*
  MODULE: CHẾ ĐỘ CHƠI
  Mỗi chế độ là một thẻ. Muốn thêm chế độ mới, copy một khối { ... } rồi sửa.
  - icon: một ký tự hoặc emoji hiện trong ô nhỏ phía trên
  - columns: số cột (2 hoặc 3)
*/
SITE.modes = {
  title: "Chế độ chơi",
  columns: 2,
  items: [
    {
      icon: "◆",
      title: "Cốt truyện",
      text: "3 màn **Thành Cổ, Bến Cảng, Pháo Đài**, mỗi màn 3 ải. Bản đồ có sàn sập, checkpoint, trùm cuối màn. Tiến độ được lưu để chơi tiếp.",
    },
    {
      icon: "✦",
      title: "Đại chiến 5v5",
      text: "Phòng trực tuyến tới 10 người, chia hai phe. Tranh bùa, phục kích trong bụi rậm, hạ tháp tiền đồn. Phe nào đủ mạng hạ gục trước thì thắng.",
    },
    {
      icon: "✧",
      title: "Đấu trường 1v1",
      text: "Tạo hoặc vào phòng bằng mã, mời bạn bè và tranh ngọc tuyệt kỹ rơi trên sàn đấu.",
    },
    {
      icon: "◆",
      title: "Hợp tác 2 người",
      text: "Cùng vượt ải trên một bản đồ chung, hồi sinh đồng đội khi bị hạ gục.",
    },
  ],
};
