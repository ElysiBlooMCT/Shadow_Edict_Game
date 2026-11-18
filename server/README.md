# Server Shadow Edict: Ám Lệnh (Socket.IO)

## v1.5 (09/11/2026) – BẮT BUỘC cập nhật lên máy chủ
Game v1.5 dùng giao thức phòng mới: chủ phòng bấm **BẮT ĐẦU** (`startGame`), phòng 5v5 tối đa 10 người,
chọn phe (`selectTeam`), chọn bản đồ (`selectMap`), chat/voice (`playerAction` type `chat`/`voice`).
Máy chủ cũ không hiểu các sự kiện này -> online "không hoạt động" (kẹt ở "Đang khởi tạo trận đấu...").
- Máy chủ v1.5 gửi `serverInfo` `{version, protocol: 2}` khi kết nối. Game hiện "● Máy chủ bản cũ – cần cập nhật server.js"
  nếu không nhận được gói này, và tạm chuyển sang cách bắt đầu cũ.
- Vẫn nhận game bản cũ: phòng có mọi người (kể cả chủ phòng) bấm Sẵn sàng thì tự bắt đầu.
- Phòng 5v5 tự cân bằng phe khi có người vào.

Kiểm tra máy chủ đang chạy bản nào: xem log khi khởi động / khi game kết nối, hoặc trong game vào sảnh 1v1:
dòng trạng thái góc phải phải là "● Trực tuyến" (không phải "Máy chủ bản cũ").

## Có gì mới so với server cũ
- **Hệ thống bạn bè** (trước đây server chưa xử lý nên 2 máy không tìm thấy nhau):
  `registerUser`, `getFriendsList`, `searchUsers`, `sendFriendRequest`, `acceptFriendRequest`,
  `declineFriendRequest`, `removeFriend`, `updatePresence`, `inviteToMatch`.
- **Định danh theo máy**: game gửi `deviceId` (ANDROID_ID trên Android, UUID trên PC).
  Cùng máy -> luôn nhận lại cùng mã `AL-XXXX`, kể cả gỡ app cài lại. Server chỉ lưu bản băm SHA-256.
- Dữ liệu người chơi lưu ở `users.json` (cùng thư mục), không mất khi khởi động lại server.
- Phòng: thêm `leaveRoom`; 1 người thoát không xóa cả phòng; quay lại sảnh sau trận không bị báo "phòng đầy"
  và chủ phòng vẫn là P1; mã phòng không bị trùng.

Giao thức phòng/đồng bộ trận (`createRoom`, `joinRoom`, `toggleReady`, `playerMove`, `playerAction`, ...) giữ nguyên.

## Cài đặt trên server (Oracle)
```bash
# chép thư mục server/ lên máy chủ, sau đó:
cd server
npm install
node server.js            # hoặc: pm2 restart <ten-app> nếu đang dùng pm2
# Nhớ DỪNG tiến trình server cũ trước (pm2 list / pm2 stop, hoặc Ctrl+C trong screen/tmux đang chạy nó)
```
Mở port 3000 (Security List của Oracle + firewall của máy chủ) như cấu hình cũ.

## Lưu ý
- Sao lưu định kỳ file `users.json`.
- `deviceId` do client tự gửi nên không chống được người cố tình giả mạo; đủ cho game đồ án.
- Trên Android, ANDROID_ID khác nhau giữa bản **debug** và bản **release** (ký bằng 2 khóa khác nhau),
  nên đổi từ debug sang release thì máy đó sẽ nhận mã AL-XXXX mới.
