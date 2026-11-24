/*
  THÔNG TIN CHUNG CỦA GAME VÀ MENU

  name: tên game, dùng khi gõ {name} trong các file data khác.
  Muốn in đậm một đoạn chữ, bọc nó trong **hai dấu sao**.
*/
SITE.server = {
  name: "Shadow Edict: Ám Lệnh",
};

/*
  LOGO Ở ĐẦU TRANG
  logo: ảnh logo game (nền trong suốt) trong thư mục assets/. width/height = kích thước thật của ảnh.
  Muốn dùng biểu tượng vuông + chữ thì thay bằng:  icon: "img/game-icon.png", text: "Shadow Edict",
  Xoá cả khối SITE.brand để dùng logo ElysiBlooMCT (assets/logo.png) như web Minecraft.
*/
SITE.brand = {
  logo: "img/game-logo.png",
  width: 720,
  height: 209,
};

/*
  BẢN PHÁT HÀNH (lấy tự động từ GitHub Releases, không cần sửa khi ra bản mới)
  repo: chủ/tên-repo trên GitHub.
  heroAssets: nút "Tải" to ở trang chủ tải thẳng file hợp với máy người xem.
              suffix = đuôi tên file trong Release, label = chữ trên nút.
*/
SITE.release = {
  repo: "ElysiBlooMCT/Shadow_Edict_Game",
  heroAssets: {
    android: { suffix: "-android.apk", label: "Tải cho Android" },
    windows: { suffix: "-windows-x64.zip", label: "Tải cho Windows" },
    linux: { suffix: "-linux-x64.zip", label: "Tải cho Linux" },
  },
};

/*
  MENU Ở ĐẦU TRANG (hiện trên mọi trang)
  Mỗi dòng là một mục: label = chữ hiện trên menu, url = địa chỉ trang, viết dạng "/tên-trang/".
  Thứ tự các dòng là thứ tự trên menu. Xoá một dòng để ẩn mục đó khỏi menu.
*/
SITE.nav = [
  { label: "Giới thiệu", url: "/about/" },
  { label: "Chế độ chơi", url: "/modes/" },
  { label: "Điều khiển", url: "/controls/" },
  { label: "Hình ảnh", url: "/gallery/" },
  { label: "Tải game", url: "/download/" },
  { label: "Changelog", url: "/changelog/" },
  { label: "FAQ", url: "/faq/" },
];
