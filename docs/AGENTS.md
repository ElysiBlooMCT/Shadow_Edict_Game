# Quy tắc cho AI / người sửa trang web Shadow Edict

Trang này dựng lại theo khung của web Minecraft ElysiBlooMCT (cùng `styles.css`, `mobile.css`, `mobile.js`, cách chia module). Quy tắc giống hệt bên đó:

- **Sau MỖI lần sửa, ghi một mục vào `AI-LOG.md`** (mục mới nhất trên cùng).
- Web tĩnh, chạy trên GitHub Pages từ thư mục `docs/` của repo game, **không có bước build**.
- File kỹ thuật nằm trong `assets/`. Mỗi trang là một thư mục (`/download/index.html`...), header/menu/chân trang do `assets/script.js` dựng.
- **Nội dung nằm trong `assets/data/*.js`**. Đổi chữ thì sửa file data, không viết cứng vào HTML/JS.
  - `site.js`: tên game, logo đầu trang, repo GitHub để lấy bản Release, menu.
  - `download.js`: các nền tảng tải. Link, phiên bản, dung lượng **tự lấy từ GitHub Releases** (khớp theo đuôi tên file `suffix`).
  - `footer.js`: chân trang, giữ giống web ElysiBlooMCT.
  - `changelog.js`: lịch sử cập nhật của game.
- Màu và font theo game (vàng kim, đỏ thẫm, nền tối; tiêu đề Cormorant Garamond có tiếng Việt), khác web Minecraft. Logo đầu trang là `assets/img/game-logo.png` tách từ ảnh nền game.
- Module mới so với web Minecraft: `download` (thẻ tải + hướng dẫn cài), `controls` (dùng chung kiểu hiển thị với `join`), `gallery` (lưới ảnh + xem ảnh lớn; trang chủ chỉ hiện số ảnh đặt ở `data-limit`). Thẻ trong `modes` có thể có ảnh (`image`). Chân trang có thêm dòng `note`.
- Ảnh chụp game nằm trong `assets/img/shots/` (bản `-s.jpg` 640px cho lưới, bản thường 1280px khi phóng to).
- Mỗi lần sửa `assets/styles.css` / `assets/script.js` / `mobile.*`: đổi `?v=...` trong mọi file `.html` (kể cả `404.html`).
- Code công khai (`assets/*.css`, `assets/*.js`, `.html`) không thêm chú thích; chú thích chỉ để trong `assets/data/*.js`.
- Web chạy ở tên miền riêng `shadow.elysibloomct.qzz.io` (file `CNAME`), nên `404.html` dùng đường dẫn tuyệt đối từ `/`. Không xoá `CNAME`.
- Thư mục `docs/**` nằm trong `paths-ignore` của workflow: sửa web không làm build lại game.
