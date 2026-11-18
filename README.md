# Shadow Edict: Ám Lệnh

Game hành động platformer 2D viết bằng [libGDX](https://libgdx.com/) 1.14 (Java 11) + Box2D, chạy trên Desktop (LWJGL3) và Android.
Có kết nối Socket.IO tới server của nhóm cho chế độ online và hệ thống bạn bè.

## Chế độ chơi

| Chế độ | Mô tả |
|--------|-------|
| **SOLO** | 5 ải màn ngang: vượt vực, leo tường, đu dây, giải cứu cậu bé, Không Tặc, Quân Triều Đình (bắn pháo), trùm Đại Thủ Lĩnh có Cuồng Huyết. Ải mở khóa được lưu lại. |
| **Đại chiến 5v5 (ARAM)** | Người chơi + 9 bot, 2 phe; bùa tranh chấp, bụi rậm phục kích, tháp tiền đồn, Thần Thú Bạo Chúa. Đủ 30 mạng thì thắng. |
| **PVP 1v1 online** | Tạo/vào phòng bằng mã, mời bạn bè, ngọc tuyệt kỹ trên sàn đấu. |
| **CO-OP 2 người** | Cùng vượt ải (bản đồ giống nhau trên 2 máy), cứu đồng đội tại Hiền Giả trên đỉnh tháp. |

## Điều khiển (desktop)

`A/D` di chuyển · `W/Space` nhảy (nhảy đúp, bật tường) · `J` / chuột trái đánh (giữ để combo) ·
`L` / chuột phải / `Shift` lướt · `U` tuyệt kỹ · `K` ứng kích từ trên cao · `F/Q` ám sát sau lưng ·
`S` ngồi / đỡ đòn (parry) · `W` (giữ) leo thang/tường · `E` bắn pháo / cứu đồng đội ·
`B` tiệm đồ · `H` uống thuốc · `P/Esc` tạm dừng. Trên Android dùng joystick ảo và các nút trên màn hình.

## Cấu trúc mã nguồn (`core/src/main/java/io/qzz/elysibloomct/ctv/se`)

- `Main` – vòng đời game, chuyển màn hình an toàn (giải phóng màn cũ ở frame sau).
- `LoadingScreen`, `MainMenuScreen`, `CharacterSelectScreen`, `LobbyScreen` (PVP), `CoopLobbyScreen`, `Arena5v5LobbyScreen` – các màn hình.
- `GameScreen` – toàn bộ gameplay (vật lý, AI quái, 5v5, HUD, vẽ).
- `Fighter5v5`, `GroundMob`, `AncientTitan`, `OutpostTurret`, `PowerUpRune`, `SwingRope`, `JumpPad`, ... – thực thể.
- `NetworkManager` (Socket.IO), `PlayerProfile` (hồ sơ & cài đặt lưu cục bộ), `SoundManager`.
- `GameAssets` (bộ đệm texture), `UiKit` (giao diện dùng chung), `SettingsDialog`, `FriendsDialog`, `MatchInviteDialog`.

Lịch sử thay đổi: xem [CHANGELOG.md](CHANGELOG.md).

## Build & chạy

- Desktop: `gradlew lwjgl3:run` – đóng gói: `gradlew lwjgl3:jar` (file ở `lwjgl3/build/libs`).
- Android: mở bằng Android Studio hoặc `gradlew android:assembleDebug` (cần Android SDK, khai báo trong `local.properties`).
- Server Socket.IO: cấu hình địa chỉ tại `NetworkManager.SERVER_URL`.

Project được tạo từ [gdx-liftoff](https://github.com/libgdx/gdx-liftoff). Các tham số Gradle hữu ích:
`--offline`, `--refresh-dependencies`, `clean`, `android:lint`.

## Tải game

Bản mới nhất cho Android, Windows, macOS, Linux: xem mục [Releases](https://github.com/ElysiBlooMCT/Shadow_Edict_Game/releases/latest).

## Bản quyền

© 2026 ElysiBlooMCT. All rights reserved.
Mã nguồn được công khai để tham khảo và học tập; không được sao chép, phân phối lại hay sử dụng mã nguồn, hình ảnh, âm thanh cho mục đích khác khi chưa có sự đồng ý của tác giả.
Dự án học tập, phi thương mại.
