# Nhật ký chỉnh sửa (AI-LOG)

Quy tắc: sau MỖI lần sửa, thêm một mục mới **lên trên cùng**. Chi tiết xem `AGENTS.md`.

---

## 2026-11-21 | Claude (sửa lệch khung trên Samsung Internet)
- Đã làm (phần game): trang /play/ khóa cuộn, tắt kéo-để-tải-lại, canvas display:block, viewport cố định; khi thanh địa chỉ ẩn/hiện (visualViewport resize) thì cuộn về 0 và báo game đổi kích thước.
- File: `teavm/.../WebLauncher.java`.
- Còn dang dở: thử lại trên Samsung Internet và Chrome Android.

---

## 2026-11-21 | Claude (logo màn tải bản web)
- Đã làm (phần game): màn hình tải của bản web dùng logo ElysiBlooMCT (`assets/web_loading_logo.png`, chép từ `docs/assets/logo.png`) thay logo libGDX.
- File: `teavm/.../WebLauncher.java`, `assets/web_loading_logo.png` (mới).

---

## 2026-11-21 | Claude (biểu tượng tab + nút Thoát trên web)
- Đã làm (phần game): trang /play/ có biểu tượng tab và tiêu đề; nút THOÁT / phím Back ở menu bản web quay về trang chủ website (../). Không sửa file trong docs.
- File: `core/.../Main.java`, `core/.../MainMenuScreen.java`, `teavm/.../WebLauncher.java`.

---

## 2026-11-21 | Claude (bảng lưu ý + xoay ngang trên web)
- Đã làm (phần game, không phải web tĩnh): bản web hiện bảng lưu ý mỗi lần mở (tải ~100 MB, chưa có micro, lưu trong trình duyệt, chạy chậm hơn, tự xoay ngang) với nút Tải ứng dụng -> ../download/; điện thoại để dọc thì game tự vẽ nằm ngang (PortraitRotation). Không sửa file nào trong docs.
- File: `core/.../Main.java`, `core/.../PortraitRotation.java`, `teavm/.../WebLauncher.java`.
- Còn dang dở: build thử bản web.

---

## 2026-11-21 | Claude (bản chơi trên trình duyệt)
- Đã làm: thêm thẻ "Chơi trên trình duyệt" (`/play/`) đầu trang Tải game và nút ở trang chủ; hỗ trợ `url` cho thẻ tải trong `assets/script.js`; hướng dẫn chơi web; FAQ chơi chung web với điện thoại, iPhone chơi qua trình duyệt; `?v=20261121a`. Kiểm tra 1280px và 360px không tràn, không lỗi JS.
- File: `assets/script.js`, `assets/data/download.js`, `assets/data/faq.js`, `index.html`, mọi `*.html`, `AGENTS.md`.
- Còn dang dở: chuyển Settings → Pages → Source sang GitHub Actions; `/play/` chỉ có khi job build web trong `pages.yml` chạy thành công.

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
