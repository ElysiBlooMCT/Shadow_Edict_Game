/*
  MODULE: ĐIỀU KHIỂN
  Mỗi nhóm (platforms) là một thẻ riêng, gồm tên và các dòng.
  Bọc chữ trong **hai dấu sao** để in đậm.
*/
SITE.controls = {
  title: "Điều khiển",
  platforms: [
    {
      name: "Máy tính: di chuyển",
      steps: [
        "**A / D** hoặc **← / →**: đi trái, phải.",
        "**W / ↑ / Space**: nhảy, nhảy đúp, bật tường, trèo mép.",
        "**R** (hoặc giữ **W** cạnh thang/tường): bám tường, leo thang.",
        "**L / Shift / chuột phải**: lướt.",
        "**S / ↓**: ngồi, chui hầm, đỡ đòn (parry).",
      ],
    },
    {
      name: "Máy tính: chiến đấu & trực tuyến",
      steps: [
        "**J / Ctrl** (giữ để nối combo) hoặc bấm **chuột trái**: tấn công.",
        "**Z / X / C** hoặc **I / O / Y**: 3 kỹ năng chủ động. **U**: tuyệt kỹ.",
        "**K / chuột giữa**: ứng kích từ trên cao. **F / Q**: ám sát sau lưng.",
        "**Tab** hoặc phím **1–5**: đổi vũ khí. **E**: bắn pháo, nói chuyện, cứu đồng đội.",
        "**B**: tiệm đồ. **H**: uống thuốc. **T**: chat, giữ **V** / bấm **M**: micro. **F1**: ẩn/hiện nút ảo. **P / Esc**: tạm dừng.",
      ],
    },
    {
      name: "Điện thoại",
      steps: [
        "Joystick ảo bên trái để di chuyển.",
        "Nút **ĐÁNH** to nhất ở góc phải, **NHẢY** bên cạnh.",
        "**LƯỚT** và **TUYỆT KỸ** phía trên nút đánh.",
        "Nút ngữ cảnh (ám sát, ứng kích, bắn pháo, nói chuyện) hiện khi dùng được.",
      ],
    },
  ],
  note: "Nút **Back** trên Android: ở menu chính là thoát game, trong trận là tạm dừng.",
};
