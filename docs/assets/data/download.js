/*
  MODULE: TẢI GAME
  Link tải, phiên bản và dung lượng được lấy TỰ ĐỘNG từ bản Release mới nhất trên GitHub
  (repo khai báo trong data/site.js). Khi phát hành bản mới không cần sửa file này.

  platforms: mỗi thẻ là một nền tảng.
    - suffix: đuôi tên file trong Release (phải khớp tên file mà GitHub Actions tạo ra)
    - os: để đánh dấu "Máy của bạn" (android, windows, mac-arm, linux)
    - url: thẻ dẫn thẳng tới một trang (không lấy file từ Release), dùng cho bản chơi trên trình duyệt
  guides: hướng dẫn cài, viết giống mục Tham gia của web Minecraft.
  Trong ready: {version} = số phiên bản, {date} = ngày phát hành.
*/
SITE.download = {
  title: "Tải game",
  yourDevice: "Máy của bạn",
  loading: "Đang lấy thông tin bản mới nhất…",
  ready: "Phiên bản {version} · phát hành ngày {date}. Bấm vào nền tảng để tải.",
  failed: "Không lấy được thông tin tự động. Xem tất cả file tại",
  allVersions: "trang Releases",
  iosNote: "iPhone / iPad: chơi bằng thẻ “Chơi trên trình duyệt” (chưa có bản cài riêng).",
  platforms: [
    { icon: "✦", name: "Chơi trên trình duyệt", text: "Không cần cài · máy tính, điện thoại, iPhone", url: "/play/", os: "ios" },
    { icon: "◆", name: "Android", text: "APK · Android 7.0 trở lên", suffix: "-android.apk", os: "android" },
    { icon: "✦", name: "Windows", text: "64-bit · chạy ShadowEdict.exe", suffix: "-windows-x64.zip", os: "windows" },
    { icon: "✧", name: "macOS Apple Silicon", text: "Mac chip M1, M2, M3…", suffix: "-macos-apple-silicon.zip", os: "mac-arm" },
    { icon: "◆", name: "macOS Intel", text: "Mac đời cũ chip Intel", suffix: "-macos-intel.zip", os: "mac-intel" },
    { icon: "✦", name: "Linux", text: "64-bit", suffix: "-linux-x64.zip", os: "linux" },
    { icon: "✧", name: "Bản JAR", text: "Mọi máy đã cài Java 11+", suffix: "-all-platforms.jar", os: "jar" },
  ],
  guides: [
    {
      name: "Trình duyệt",
      steps: [
        "Bấm **Chơi trên trình duyệt** và đợi tải xong (lần đầu khá lâu, các lần sau nhanh hơn).",
        "Chơi được cả online, vào chung phòng với người chơi điện thoại và máy tính.",
        "Bản web chưa có đàm thoại micro, vẫn chat chữ bình thường. Nên dùng Chrome, Edge, Firefox hoặc Safari bản mới.",
      ],
    },
    {
      name: "Android",
      steps: [
        "Tải file **.apk** và mở nó.",
        "Nếu máy hỏi, cho phép cài ứng dụng từ nguồn này.",
        "Đã có bản trước thì cứ cài đè lên, **không mất dữ liệu**.",
      ],
    },
    {
      name: "Windows / Linux",
      steps: [
        "Tải file **.zip** và giải nén ra một thư mục.",
        "Chạy **ShadowEdict.exe** (Linux: file **ShadowEdict**). Không cần cài Java.",
        "Windows SmartScreen cảnh báo thì chọn **More info → Run anyway**.",
      ],
    },
    {
      name: "macOS",
      steps: [
        "Tải đúng bản theo chip của máy và giải nén.",
        "Game chưa được Apple ký nên lần đầu macOS báo app bị hỏng.",
        "Mở Terminal và chạy: **xattr -cr ~/Downloads/\"Shadow Edict.app\"**",
        "Hoặc chuột phải vào app → **Open** → **Open**.",
      ],
    },
  ],
};
