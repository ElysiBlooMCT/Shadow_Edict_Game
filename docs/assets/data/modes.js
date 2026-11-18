/*
  MODULE: CHẾ ĐỘ CHƠI
  Mỗi chế độ là một thẻ. Muốn thêm chế độ mới, copy một khối { ... } rồi sửa.
  - icon: một ký tự hoặc emoji hiện trong ô nhỏ phía trên
  - columns: số cột (2 hoặc 3)
  - image: ảnh nhỏ trên đầu thẻ (assets/img/...), imageFull: ảnh lớn khi bấm vào, caption: chú thích ảnh lớn.
    Xoá 3 dòng này nếu không muốn thẻ có ảnh.
*/
SITE.modes = {
  title: "Chế độ chơi",
  columns: 2,
  items: [
    {
      icon: "◆",
      image: "img/shots/ben-cang-s.jpg",
      imageFull: "img/shots/ben-cang.jpg",
      caption: "Màn 2 · Bến Cảng",
      title: "Cốt truyện",
      text: "3 màn **Thành Cổ, Bến Cảng, Pháo Đài**, mỗi màn 3 ải. Bản đồ có sàn sập, checkpoint, trùm cuối màn. Tiến độ được lưu để chơi tiếp.",
    },
    {
      icon: "✦",
      image: "img/shots/dai-chien-5v5-s.jpg",
      imageFull: "img/shots/dai-chien-5v5.jpg",
      caption: "Tung tuyệt kỹ giữa trận 5v5",
      title: "Đại chiến 5v5",
      text: "Phòng trực tuyến tới 10 người, chia hai phe. Tranh bùa, phục kích trong bụi rậm, hạ tháp tiền đồn. Phe nào đủ mạng hạ gục trước thì thắng.",
    },
    {
      icon: "✧",
      image: "img/shots/dau-truong-1v1-s.jpg",
      imageFull: "img/shots/dau-truong-1v1.jpg",
      caption: "Sảnh đấu trường 1v1",
      title: "Đấu trường 1v1",
      text: "Tạo hoặc vào phòng bằng mã, mời bạn bè và tranh ngọc tuyệt kỹ rơi trên sàn đấu.",
    },
    {
      icon: "◆",
      image: "img/shots/phao-dai-s.jpg",
      imageFull: "img/shots/phao-dai.jpg",
      caption: "Cùng vượt ải trên Pháo Đài",
      title: "Hợp tác 2 người",
      text: "Cùng vượt ải trên một bản đồ chung, hồi sinh đồng đội khi bị hạ gục.",
    },
  ],
};
