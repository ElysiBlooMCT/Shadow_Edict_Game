# Nhật ký chỉnh sửa (AI-LOG)

Quy tắc: sau MỖI lần sửa, thêm một mục mới **lên trên cùng**. Chi tiết xem `AGENTS.md`.

---

## 2026-11-18 | Claude (thêm ảnh, dòng AI)
- Đã làm: chụp 12 ảnh game (menu, 3 màn cốt truyện, thoại, 5v5, chọn sát thủ, chọn màn, tiệm đồ, sảnh 5v5, 1v1) vào `assets/img/shots/`; module mới `gallery` (trang `/gallery/`, 6 ảnh ở trang chủ, bấm xem ảnh lớn, phím trái/phải chuyển ảnh); thẻ chế độ chơi có ảnh; menu thêm mục Hình ảnh; chân trang thêm dòng ghi chú dùng AI hỗ trợ (`note` trong `footer.js`); `?v=20261118c`. Kiểm tra 1280px và 360px không tràn, không lỗi JS.
- File: `assets/script.js`, `assets/styles.css`, `assets/data/gallery.js` (mới), `modes.js`, `footer.js`, `site.js`, `index.html`, `gallery/index.html` (mới), mọi `*.html`.
- Còn dang dở: ảnh chụp từ bản máy tính có hiện nút cảm ứng (chế độ thử nghiệm); có thể thay ảnh chụp từ điện thoại thật.

---

## 2026-11-18 | Claude (tên miền riêng)
- Đã làm: web chuyển sang tên miền `shadow.elysibloomct.qzz.io`; `404.html` đổi đường dẫn `/Shadow_Edict_Game/` thành `/`; cập nhật `AGENTS.md`.
- File: `404.html`, `AGENTS.md`, `AI-LOG.md`.
- Còn dang dở: bật Enforce HTTPS sau khi GitHub cấp chứng chỉ.

---

## 2026-11-18 | Claude (đổi màu theo game)
- Đã làm: đổi bảng màu sang vàng kim / đỏ thẫm / tím hoàng hôn lấy từ ảnh game; font tiêu đề Cormorant Garamond thay VT323 (bỏ thư mục fonts); logo đầu trang và trang chủ dùng logo "Shadow Edict / Ám Lệnh" tách từ ảnh nền (`assets/img/game-logo.png`); ảnh trong thẻ phiên bản cắt bỏ phần chữ (`assets/img/scene.jpg`); icon ♡ đổi thành ◆; `?v=20261118b`. Chân trang giữ logo ElysiBlooMCT như cũ.
- File: `assets/styles.css`, `assets/mobile.css`, `assets/script.js`, `assets/data/site.js`, `home.js`, `modes.js`, `download.js`, mọi `*.html`, `AGENTS.md`.
- Còn dang dở: như mục dưới.

---

## 2026-11-18 | Claude
- Đã làm: dựng trang web game theo khung web Minecraft ElysiBlooMCT (theme hồng/đỏ mận, font VT323, menu ba gạch trên điện thoại, module data). Chân trang giống web ElysiBlooMCT (logo, "Crafted with pink magic"). Trang: chủ, giới thiệu, chế độ chơi, điều khiển, tải game (tự lấy bản mới nhất từ GitHub Releases, đánh dấu "Máy của bạn"), changelog, FAQ, 404. `?v=20261118a`. Đã kiểm tra ở 1280px và 360px: không tràn ngang, không lỗi JS.
- File: toàn bộ `docs/`.
- Còn dang dở: chủ web bật GitHub Pages (Settings → Pages → main /docs); ảnh nền giống Assassin's Creed, nên thay ảnh tự vẽ trước khi quảng bá rộng.
