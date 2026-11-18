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
        "**A / D**: đi trái, phải.",
        "**W / Space**: nhảy, nhảy đúp, bật tường.",
        "**Giữ W**: leo thang, leo tường.",
        "**L / Shift / chuột phải**: lướt.",
        "**S**: ngồi, đỡ đòn (parry).",
      ],
    },
    {
      name: "Máy tính: chiến đấu",
      steps: [
        "**J / chuột trái**: đánh, giữ để ra combo.",
        "**U**: tuyệt kỹ. **K**: ứng kích từ trên cao.",
        "**F / Q**: ám sát sau lưng.",
        "**Tab**: đổi vũ khí. **E**: bắn pháo, nói chuyện, cứu đồng đội.",
        "**B**: tiệm đồ. **H**: uống thuốc. **P / Esc**: tạm dừng.",
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
