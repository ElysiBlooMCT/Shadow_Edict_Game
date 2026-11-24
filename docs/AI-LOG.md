# Nhật ký chỉnh sửa (AI-LOG)

Quy tắc: sau MỖI lần sửa, thêm một mục mới **lên trên cùng**. Chi tiết xem `AGENTS.md`.

---

## 2026-11-25 | Antigravity (rà soát & sửa lỗi toàn diện game, server, bản web và website)
- Đã làm (phần game – lõi, chiến đấu, cốt truyện, trực tuyến):
  - **GameScreen & chiến đấu**: sửa lỗi nhân đôi vàng vô hạn giữa `StorySaveManager` và `PlayerProfile` (chuyển vàng chưa ngân hàng hóa sang `PlayerProfile` trước khi lưu và lưu `savedGold = 0`); sửa kẹt mềm CO-OP khi gục ngã (`isMatchEnded` chỉ bật khi cả đội gục); sửa lỗi 5v5 đa người chơi (`extraPeers5v5`) bị bỏ qua ở Tuyệt kỹ P1/P2, Rìu/Song Đao/Lướt/Ám sát/Ưng kích/Cung khóa mục tiêu/Vạn Kiếm/Huyết Trận/Phân Thân; sửa bùa `RANDOM_ULTIMATE` không đổi tuyệt kỹ khi bấm U; sửa lặp vô hạn sát thương phản chấn Lôi Phong Hộ Thể và xóa sát thương kép phía client của đạn/phân thân/huyết trận từ đối thủ; sửa độc của Vân U áp dụng đúng giảm tốc (qua `playerPoisonSlowTimer`), giảm 85% sát thương khi Trùm gồng, không gây choáng và không nhân đôi chữ sát thương; sửa lỗi Trùm bị khóa choáng vô hạn và ghi đè trạng thái `BERSERK_CHARGE`; sửa Đập đất của Trùm thiếu kiểm tra trục Y; sửa lính cận chiến đánh ngược hướng & lính gác giết Tiểu Vũ khi cậu bé đang đứng yên chờ cứu; sửa khai thác hồi chiêu 0,5s Phân Thân Hắc Ám khi nổ sớm; sửa Ưng kích gọi `stopEagleStrike` trong callback Box2D và xuyên tường biên; sửa bám mép (`performLedgeMantle`) và đu dây trên cảm ứng; bật lại bệ nhảy/bùa/bụi rậm trong CO-OP; sửa Rìu/Song Đao thiếu kiểm tra hướng đánh và làm giảm bonus kiếm khi nhặt hòm vũ khí; đóng băng vật lý/đạn địch khi đang đọc thoại hoặc mở tiệm đồ chơi đơn; khóa tiệm đồ/uống thuốc ở 1v1; sửa thứ tự lớp phủ Pause/GameOver đè lên tiệm đồ & chat; sửa rò rỉ `Pixmap` (42 texture procedural chuyển sang `managedTex()` và giải phóng trong `dispose()`); sửa lệch vị trí pháo ải 3-3 và thang nổi; sửa Tiểu Vũ/quái sinh trong hầm bò ải 1-2.
  - **Thực thể & hệ thống lõi**: sửa `AncientTitan` gây sát thương AoE mỗi khung hình; sửa `Fighter5v5` (lỗi dead-code hạ cánh P1 Ult, lỗi ghim `attackTimer` khi P2 tàng hình, giới hạn tầm nhìn 18 m và tôn trọng tàng hình/bụi rậm, tránh lẫn hệ số bùa); sửa `StorySaveManager.clearSave()` trên TeaVM `WebPreferences`; cô lập đọc từng khóa trong `PlayerProfile` & tránh xóa dữ liệu khi `flush()` lỗi; sửa rò rỉ `Pixmap` trong `GameAssets` và `MapSelectScreen`; sửa `PortraitRotation` (FBO thất bại, khôi phục Graphics/Input khi `swaps > 0`, reset trạng thái trong `dispose()`); sửa `SoundManager` (tránh NPE `HowlMusic` trên TeaVM khi `reloadMenuBgm`, cô lập lỗi nạp từng âm thanh); sửa `UiKit` không thu nhỏ font fallback khi thiếu `.fnt`; thay ký tự thiếu glyph (`↑ ↓ ► ◄`) trong `LanguageManager` và `GameScreen`; sửa `SwingRope`, `HealthBar`, `FriendInfo`, `TextFmt`, `DeviceIdentity`.
  - **Màn hình & sảnh trực tuyến**: sửa `LobbyScreen`, `CoopLobbyScreen`, `Arena5v5LobbyScreen` (không để `hide()` xóa mất listener `"playerLeft"` của `GameScreen`, không còn đảo trạng thái Sẵn sàng 5v5, không ghi đè cân bằng phe 5v5 khi rejoin, đồng bộ `selectedMap`, tiêu thụ phím Enter khỏi ô nhập); sửa `MatchInviteDialog` rời phòng cũ và truyền `pendingJoinCode`; sửa `FriendsDialog` giữ tab tìm kiếm, vị trí cuộn và danh sách đã mời khi làm mới 3s; sửa `PlayerProfileDialog` gỡ listener khi đóng và cập nhật đúng cấp bậc; sửa `CharacterSelectScreen` không xóa save sớm khi bấm Chơi mới và không ghi đè tiến độ cốt truyện khi chơi màn tự do; chuẩn hóa kiểm tra lớp trên cùng (`UiKit.isTopModal`) cho mọi hộp thoại.
  - **Mạng, Voice/Chat, Nền tảng & Máy chủ**: sửa `NetworkManager` (khớp đúng `socket.id()` trước `userId`, reset `currentRoomMode`, thêm `shutdown()`); sửa `OnlineCommsManager` (nhả micro phần cứng khi tắt/im lặng sau 0,5s, lọc `\r\n` & giới hạn gói voice, phát liên tục khi giữ Push-To-Talk, hạ nhịp xin quyền micro); sửa `Lwjgl3Launcher` + `JavaGameSocket` (daemon thread + `System.exit(0)` không còn treo tiến trình PC); sửa `AndroidLauncher` (tránh rò rỉ Activity context) & `AndroidManifest.xml` (`hardware.microphone` `required="false"`); sửa bản web `WebLauncher`/`WebGameSocket`/`JSONObject`/`JSONArray` (đổi `window.prompt()` chặn luồng thành hộp nhập HTML bất đồng bộ không gây timeout socket 5s, phát hiện cảm ứng theo `(pointer: coarse)`, nạp lại thư viện socket.io khi lỗi CDN); sửa `server/server.js` (chống Prototype Pollution bằng `Object.create(null)`, tuần tự hóa ghi `users.json` + lưu đồng bộ khi `SIGINT`/`SIGTERM`, gộp người chơi kết nối lại theo `userId`, kiểm tra dữ liệu đầu vào `registerUser`/`chat`/`voice`, yêu cầu đủ 2 người cho 1v1/CO-OP trong `startGame`).
- Đã làm (phần web `docs/` & workflow):
  - Thêm `'docs/**'` vào `paths-ignore` trong `.github/workflows/release.yml` theo đúng `AGENTS.md`.
  - Sửa `docs/assets/script.js` và `docs/assets/styles.css`: thêm `.lightbox-nav[hidden] { display: none; }`; chặn phím mũi tên khi lightbox chỉ có 1 ảnh và kiểm tra hỗ trợ `<dialog>` trước khi tạo DOM; sửa `detectOs()` nhận diện iPadOS 13+ là `ios` và macOS là `mac` (bỏ gán cứng `mac-arm` trong `site.js` để Mac Intel không tải nhầm bản ARM64); xử lý hiển thị dự phòng khi GitHub Releases API lỗi ở `fillRelease()` và giữ nguyên `iosNote` cho iOS ở `renderDownload()`.
  - Sửa dữ liệu: `modes.js` (cập nhật mô tả 5v5 thành hạ Thần Thú Bạo Chúa thay vì tháp tiền đồn), `controls.js` (cập nhật đầy đủ phím kỹ năng Z/X/C, I/O/Y, bám tường R, đổi vũ khí 1–5, chat T, micro V/M, F1), `gallery.js` (sửa lỗi chính tả `"Khói binh"` → `"Khởi binh"`), `changelog.js` (thêm mục `v1.5.3`).
  - Cập nhật cả 9 file `.html` (kể cả `404.html`): chuyển `id="top"` từ `<header>` dính sang `<body id="top">`, thêm `class="panel-date"` cho `#release-date` ở `index.html`, đổi `og:image` sang URL tuyệt đối `https://shadow.elysibloomct.qzz.io/assets/img/background.jpg`, và nâng cache-busting lên `?v=20261125a`.
- File: 55 file trong `core/`, `lwjgl3/`, `android/`, `teavm/`, `server/server.js`, `.github/workflows/release.yml`, `docs/` (`styles.css`, `script.js`, `data/*.js`, cả 9 file `.html`), `CHANGELOG.md`, `docs/AI-LOG.md`.
- Kiểm tra: biên dịch thành công 0 lỗi `:core:compileJava`, `:lwjgl3:compileJava`, `:teavm:compileJava` (`-Pweb`) và đóng gói đầy đủ bản web TeaVM (`:teavm:gdx_teavm_web_js_build`); kiểm tra `docs/` không có chú thích trong code công khai, `CNAME` nguyên vẹn, `?v=20261125a` đồng nhất trên cả 9 file HTML; rà soát thủ công `server/server.js`, `AndroidLauncher.java`, `AndroidManifest.xml`.
- Còn dang dở: môi trường máy hiện tại chưa có Android SDK (`local.properties` trỏ sang `D:\Project\Program\ASDK`) và chưa cài `node`, cần build thử APK / khởi động lại `server/server.js` trên máy chủ khi triển khai; nếu muốn tự tạo GitHub Release mới, nâng `projectVersion` trong `gradle.properties` lên `1.5.3`.

---

## 2026-11-21 | Claude (bàn phím điện thoại trên bản web)
- Đã làm (phần game): trên web điện thoại, chạm vào ô nhập chữ (đổi tên, tìm bạn, mã phòng, tên 5v5, chat) mở hộp nhập của trình duyệt để hiện bàn phím; bấm OK thì điền chữ (cắt theo độ dài tối đa) và xử lý như bấm Enter. PC, Android, web máy tính không đổi.
- File: `core/.../TextInputBridge.java` (mới), `Arena5v5LobbyScreen.java`, `LobbyScreen.java`, `FriendsDialog.java`, `GameScreen.java`, `teavm/.../WebLauncher.java`.
- Kiểm tra: biên dịch core + teavm; chạy thử trên desktop với hộp nhập giả (chạm → hỏi → điền chữ → Enter).
- Còn dang dở: thử trên Chrome/Samsung Internet điện thoại.

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
