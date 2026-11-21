package io.qzz.elysibloomct.ctv.se;

import com.badlogic.gdx.Gdx;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NetworkManager {
    private static NetworkManager instance;
    /** Địa chỉ server Socket.IO (Oracle Cloud của nhóm). */
    public static final String SERVER_URL = System.getProperty("al.server", "https://game.elysibloomct.qzz.io");
    /**
     * Cách tạo kết nối của từng nền tảng: PC/Android gán JavaGameSocket::create, web gán WebGameSocket::new.
     * Phải gán trước khi NetworkManager.getInstance() được gọi lần đầu.
     */
    public static GameSocket.Factory socketFactory;
    public GameSocket socket;

    // Lưu thông tin phòng & nhân vật
    public String currentRoomCode = "";
    public int myPlayerIndex = 1; // 1: Chủ phòng (P1), 2: Khách (P2)
    public int myCharacter = 1;   // 1: Sát thủ trắng, 2: Sát thủ đen
    public int partnerCharacter = 1; // Nhân vật của đối thủ hoặc đồng đội (1: Trắng, 2: Đen)
    public int partnerTeam = 2;      // Phe của đối thủ/đồng đội chính trong 5v5
    public String partnerName = "Đối thủ";
    public String playerName = "Chiến Thần";
    public int myTeam = 1; // 1: Phe Ánh Sáng (Team Blue), 2: Phe Bóng Tối (Team Red)
    public int selectedMap = 1; // 1: Thành Cổ Thời Chiến, 2: Bến Cảng Ban Đêm, 3: Pháo Đài Trên Núi

    // Phiên bản giao thức máy chủ. Máy chủ v1.5+ gửi "serverInfo" ngay khi kết nối.
    // Máy chủ cũ (v1.4 trở về trước) không gửi -> game chuyển sang chế độ tương thích:
    // trận bắt đầu khi cả 2 người bấm Sẵn sàng (toggleReady), phòng tối đa 2 người.
    public static final int REQUIRED_PROTOCOL = 2;
    public volatile int serverProtocol = 0;
    public volatile String serverVersion = "";
    private volatile long connectedAtMs = 0L;

    // Dữ liệu bạn bè & lời mời
    public final List<FriendInfo> friendsList = new ArrayList<>();
    public final List<FriendInfo> searchResults = new ArrayList<>();
    public final List<String> pendingFriendRequests = new ArrayList<>();
    /** Tên người gửi lời mời kết bạn (userId -> tên) để hiển thị thay vì chỉ có mã. */
    public final java.util.Map<String, String> pendingRequestNames = new java.util.HashMap<>();
    public MatchInvite currentMatchInvite = null;
    /** Thời điểm nhận lời mời (ms) - lời mời quá 60s sẽ không hiện lại khi đổi màn hình. */
    private long currentInviteTime = 0L;
    private static final long INVITE_TTL_MS = 60_000L;

    // Callback listeners cho giao diện UI
    public interface FriendsUpdateListener {
        void onFriendsUpdated();
        void onSearchResultsUpdated();
        void onFriendRequestReceived(String requesterId, String requesterName);
    }

    public interface MatchInviteListener {
        void onMatchInviteReceived(MatchInvite invite);
    }

    private final List<FriendsUpdateListener> friendsListeners = new ArrayList<>();
    private MatchInviteListener matchInviteListener;

    public void addFriendsListener(FriendsUpdateListener listener) {
        if (listener != null && !friendsListeners.contains(listener)) {
            friendsListeners.add(listener);
        }
    }

    public void removeFriendsListener(FriendsUpdateListener listener) {
        friendsListeners.remove(listener);
    }

    public void setMatchInviteListener(MatchInviteListener listener) {
        this.matchInviteListener = listener;
        // Chỉ hiện lại lời mời còn hiệu lực (trước đây lời mời bị bỏ qua sẽ hiện lại mãi ở mọi màn hình)
        if (currentMatchInvite != null && listener != null) {
            if (System.currentTimeMillis() - currentInviteTime <= INVITE_TTL_MS) {
                listener.onMatchInviteReceived(currentMatchInvite);
            } else {
                currentMatchInvite = null;
            }
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    /**
     * Máy chủ đang chạy bản cũ (không gửi "serverInfo" trong 2,5 giây đầu sau khi kết nối).
     * Khi đó lệnh "startGame" của chủ phòng bị bỏ qua, nên lobby phải dùng cách bắt đầu cũ.
     */
    public boolean isLegacyServer() {
        return isConnected() && serverProtocol < REQUIRED_PROTOCOL
            && connectedAtMs > 0 && System.currentTimeMillis() - connectedAtMs > 2500;
    }

    /** Thử kết nối lại nếu đang mất kết nối (an toàn khi gọi nhiều lần). */
    public void ensureConnected() {
        try {
            if (socket != null && !socket.connected()) socket.connect();
        } catch (Exception ignored) {}
    }

    /**
     * Báo server rời phòng hiện tại và xóa mã phòng cục bộ.
     * Nếu server chưa xử lý sự kiện "leaveRoom" thì socket.io bỏ qua, không gây lỗi.
     */
    public void leaveRoom() {
        String code = currentRoomCode;
        if (socket != null && socket.connected() && code != null && !code.isEmpty()) {
            try { socket.emit("leaveRoom", code); } catch (Exception ignored) {}
        }
        currentRoomCode = "";
        updateMyPresence("ONLINE", "", "");
    }

    /**
     * Đồng bộ nhân vật, phe và tên của đối thủ/đồng đội từ danh sách người chơi trong phòng.
     * Ưu tiên chọn đối thủ khác phe trong chế độ 5v5, nếu cùng phe thì lấy đồng đội đầu tiên.
     */
    public void syncRoomPlayers(JSONArray players, String localUserId) {
        if (players == null || players.length() == 0) return;
        String mySocketId = (socket != null && socket.id() != null) ? socket.id() : "";
        int selfIdx = -1;
        for (int i = 0; i < players.length(); i++) {
            JSONObject p = players.optJSONObject(i);
            if (p == null) continue;
            String pid = p.optString("id", "");
            String puid = p.optString("userId", "");
            if (!mySocketId.isEmpty() && mySocketId.equals(pid)) {
                selfIdx = i;
                break;
            }
            if (localUserId != null && !localUserId.isEmpty() && localUserId.equals(puid)) {
                selfIdx = i;
                break;
            }
        }
        if (selfIdx < 0 && myPlayerIndex >= 1 && myPlayerIndex <= players.length()) {
            selfIdx = myPlayerIndex - 1;
        }
        if (selfIdx >= 0) {
            myPlayerIndex = selfIdx + 1;
            JSONObject selfObj = players.optJSONObject(selfIdx);
            if (selfObj != null) {
                if (selfObj.has("charId")) {
                    myCharacter = (selfObj.optInt("charId", 1) == 2) ? 2 : 1;
                }
                if (selfObj.has("team")) {
                    myTeam = (selfObj.optInt("team", 1) == 2) ? 2 : 1;
                }
            }
        }

        JSONObject chosenOther = null;
        for (int i = 0; i < players.length(); i++) {
            if (i == selfIdx) continue;
            JSONObject p = players.optJSONObject(i);
            if (p == null) continue;
            if (chosenOther == null) {
                chosenOther = p;
            }
            int pTeam = p.optInt("team", 2);
            if (pTeam != myTeam) {
                chosenOther = p;
                break;
            }
        }
        if (chosenOther != null) {
            partnerCharacter = (chosenOther.optInt("charId", 1) == 2) ? 2 : 1;
            partnerTeam = (chosenOther.optInt("team", myTeam == 1 ? 2 : 1) == 2) ? 2 : 1;
            String n = chosenOther.optString("name", "");
            if (!n.isEmpty()) partnerName = n;
        }
    }

    private NetworkManager() {
        initSocket();
    }

    private void initSocket() {
        try {
            // Đọc tên người chơi từ PlayerProfile nếu có
            if (PlayerProfile.getInstance() != null && PlayerProfile.getInstance().playerName != null) {
                playerName = PlayerProfile.getInstance().playerName;
            }

            if (socketFactory == null) {
                System.out.println("NetworkManager: Nen tang nay chua gan socketFactory -> choi offline.");
                return;
            }
            // Kết nối tới server game (qua Cloudflare)
            socket = socketFactory.create(SERVER_URL);

            socket.on("serverInfo", args -> {
                try {
                    if (args.length > 0 && args[0] instanceof JSONObject) {
                        JSONObject info = (JSONObject) args[0];
                        serverProtocol = info.optInt("protocol", 1);
                        serverVersion = info.optString("version", "");
                        System.out.println("NetworkManager: Server v" + serverVersion + " (protocol " + serverProtocol + ")");
                    }
                } catch (Exception ignored) {}
            });

            socket.on(GameSocket.EVENT_CONNECT, args -> {
                connectedAtMs = System.currentTimeMillis();
                serverProtocol = 0;
                System.out.println("NetworkManager: Connected to Oracle server successfully via WebSocket!");
                registerUserWithServer();
                refreshFriendsList();
                if (!currentRoomCode.isEmpty()) {
                    try {
                        JSONObject joinData = new JSONObject();
                        joinData.put("name", playerName);
                        joinData.put("charId", myCharacter);
                        joinData.put("team", myTeam);
                        joinData.put("userId", PlayerProfile.getInstance().userId);
                        socket.emit("joinRoom", currentRoomCode, joinData);
                        socket.emit("getRoomState");
                    } catch (Exception ignored) {}
                }
            }).on(GameSocket.EVENT_CONNECT_ERROR, args -> {
                System.out.println("NetworkManager: Connection error -> " + (args.length > 0 ? args[0] : "Timeout/Server down"));
            }).on(GameSocket.EVENT_DISCONNECT, args -> {
                System.out.println("NetworkManager: Disconnected from server. Trying to reconnect...");
            });

            setupSocialEvents();

            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("NetworkManager: Khong tao duoc ket noi");
        }
    }

    private void setupSocialEvents() {
        if (socket == null) return;

        // Nhận dữ liệu hồ sơ sau khi đăng ký
        socket.on("userRegistered", args -> Gdx.app.postRunnable(() -> {
            try {
                if (args.length > 0 && args[0] instanceof JSONObject) {
                    JSONObject data = (JSONObject) args[0];
                    String uId = data.optString("userId", "");
                    String uName = data.optString("name", playerName);
                    int gold = data.optInt("gold", 500);
                    int lvl = data.optInt("level", 1);
                    PlayerProfile.getInstance().updateFromServer(uId, uName, gold, lvl);
                    playerName = uName;
                    System.out.println("NetworkManager: Ho so dong bo thanh cong -> ID: " + uId + " | Name: " + uName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        // Nhận danh sách bạn bè kèm trạng thái Realtime
        socket.on("friendsList", args -> Gdx.app.postRunnable(() -> {
            try {
                if (args.length > 0 && args[0] instanceof JSONArray) {
                    JSONArray arr = (JSONArray) args[0];
                    friendsList.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        FriendInfo fi = new FriendInfo();
                        fi.userId = obj.optString("userId", "");
                        fi.name = obj.optString("name", "Ban");
                        fi.level = obj.optInt("level", 1);
                        fi.status = obj.optString("status", "OFFLINE");
                        fi.roomCode = obj.optString("roomCode", "");
                        fi.mode = obj.optString("mode", "");
                        fi.isFriend = true;
                        friendsList.add(fi);
                    }
                    notifyFriendsUpdated();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        // Nhận cập nhật trạng thái của bạn bè Realtime
        socket.on("friendPresenceUpdate", args -> Gdx.app.postRunnable(() -> {
            try {
                if (args.length > 0 && args[0] instanceof JSONObject) {
                    JSONObject obj = (JSONObject) args[0];
                    String uId = obj.optString("userId", "");
                    String status = obj.optString("status", "OFFLINE");
                    String roomCode = obj.optString("roomCode", "");
                    String mode = obj.optString("mode", "");

                    boolean found = false;
                    for (FriendInfo fi : friendsList) {
                        if (fi.userId.equals(uId)) {
                            fi.status = status;
                            fi.roomCode = roomCode;
                            fi.mode = mode;
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        notifyFriendsUpdated();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        // Nhận kết quả tìm kiếm người chơi
        socket.on("searchResults", args -> Gdx.app.postRunnable(() -> {
            try {
                if (args.length > 0 && args[0] instanceof JSONArray) {
                    JSONArray arr = (JSONArray) args[0];
                    searchResults.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        FriendInfo fi = new FriendInfo();
                        fi.userId = obj.optString("userId", "");
                        fi.name = obj.optString("name", "");
                        fi.level = obj.optInt("level", 1);
                        fi.isFriend = obj.optBoolean("isFriend", false);
                        fi.isPending = obj.optBoolean("isPending", false);
                        searchResults.add(fi);
                    }
                    notifySearchResultsUpdated();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        // Nhận lời mời kết bạn Realtime
        socket.on("friendRequestReceived", args -> Gdx.app.postRunnable(() -> {
            try {
                if (args.length > 0 && args[0] instanceof JSONObject) {
                    JSONObject obj = (JSONObject) args[0];
                    String reqId = obj.optString("requesterId", "");
                    String reqName = obj.optString("requesterName", "Nguoi la");
                    if (!pendingFriendRequests.contains(reqId)) {
                        pendingFriendRequests.add(reqId);
                    }
                    pendingRequestNames.put(reqId, reqName);
                    for (FriendsUpdateListener l : new ArrayList<>(friendsListeners)) {
                        l.onFriendRequestReceived(reqId, reqName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        // Kết bạn thành công
        socket.on("friendRequestAccepted", args -> Gdx.app.postRunnable(this::refreshFriendsList));
        socket.on("friendRemoved", args -> Gdx.app.postRunnable(this::refreshFriendsList));

        // Nhận lời mời vào trận Realtime
        socket.on("matchInviteReceived", args -> Gdx.app.postRunnable(() -> {
            try {
                if (args.length > 0 && args[0] instanceof JSONObject) {
                    JSONObject obj = (JSONObject) args[0];
                    String inviterId = obj.optString("inviterId", "");
                    String inviterName = obj.optString("inviterName", "Ban be");
                    String roomCode = obj.optString("roomCode", "");
                    String mode = obj.optString("mode", "1v1");
                    String msg = obj.optString("message", "");

                    currentMatchInvite = new MatchInvite(inviterId, inviterName, roomCode, mode, msg);
                    currentInviteTime = System.currentTimeMillis();
                    System.out.println("NetworkManager: Nhan loi moi vao phong " + roomCode + " tu " + inviterName);

                    if (matchInviteListener != null) {
                        matchInviteListener.onMatchInviteReceived(currentMatchInvite);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public void registerUserWithServer() {
        if (socket != null && socket.connected()) {
            try {
                PlayerProfile profile = PlayerProfile.getInstance();
                JSONObject data = new JSONObject();
                // deviceId cố định theo máy -> server cấp lại đúng mã AL-XXXX cũ sau khi cài lại app
                data.put("deviceId", DeviceIdentity.get());
                data.put("userId", profile.userId); // mã cũ đang lưu (server ưu tiên giữ nếu chưa ai dùng)
                data.put("name", profile.playerName);
                data.put("level", profile.level);
                data.put("gold", profile.gold);
                data.put("matches", profile.matches);
                data.put("wins", profile.wins);
                data.put("favoriteHero", profile.favoriteHero != null ? profile.favoriteHero : "Vân Tiêu");
                socket.emit("registerUser", data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void getUserProfile(String targetUserId) {
        if (socket != null && socket.connected() && targetUserId != null) {
            socket.emit("getUserProfile", targetUserId);
        }
    }

    public void refreshFriendsList() {
        if (socket != null && socket.connected()) {
            socket.emit("getFriendsList");
        }
    }

    public void searchUsers(String query) {
        if (socket != null && socket.connected() && query != null && !query.trim().isEmpty()) {
            socket.emit("searchUsers", query.trim());
        }
    }

    public void sendFriendRequest(String targetUserId) {
        if (socket != null && socket.connected() && targetUserId != null) {
            socket.emit("sendFriendRequest", targetUserId);
        }
    }

    public void acceptFriendRequest(String requesterId) {
        if (socket != null && socket.connected() && requesterId != null) {
            pendingFriendRequests.remove(requesterId);
            socket.emit("acceptFriendRequest", requesterId);
        }
    }

    public void declineFriendRequest(String requesterId) {
        if (socket != null && socket.connected() && requesterId != null) {
            pendingFriendRequests.remove(requesterId);
            socket.emit("declineFriendRequest", requesterId);
        }
    }

    public void removeFriend(String friendId) {
        if (socket != null && socket.connected() && friendId != null) {
            socket.emit("removeFriend", friendId);
        }
    }

    public void inviteFriendToMatch(String targetUserId, String roomCode, String mode) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("targetUserId", targetUserId);
                data.put("roomCode", roomCode);
                data.put("mode", mode);
                socket.emit("inviteToMatch", data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateMyPresence(String status, String roomCode, String mode) {
        if (socket != null && socket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("status", status); // "ONLINE", "IN_ROOM", "IN_GAME"
                data.put("roomCode", roomCode != null ? roomCode : "");
                data.put("mode", mode != null ? mode : "");
                socket.emit("updatePresence", data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Duyệt trên bản sao: listener có thể tự gỡ mình trong callback (tránh ConcurrentModificationException)
    private void notifyFriendsUpdated() {
        for (FriendsUpdateListener l : new ArrayList<>(friendsListeners)) {
            l.onFriendsUpdated();
        }
    }

    private void notifySearchResultsUpdated() {
        for (FriendsUpdateListener l : new ArrayList<>(friendsListeners)) {
            l.onSearchResultsUpdated();
        }
    }

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
}
